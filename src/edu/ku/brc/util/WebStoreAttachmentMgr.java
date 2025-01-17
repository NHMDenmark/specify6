/* Copyright (C) 2020, Specify Collections Consortium
 * 
 * Specify Collections Consortium, Biodiversity Institute, University of Kansas,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA, support@specifysoftware.org
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.SwingUtilities;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.helpers.XMLHelper;
import edu.ku.brc.helpers.ProxyHelper;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.Attachment;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.Institution;
import edu.ku.brc.ui.IconEntry;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.util.thumbnails.Thumbnailer;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Nov 1, 2011
 *
 */
public class WebStoreAttachmentMgr implements AttachmentManagerIface
{
    private static final Logger  log   = Logger.getLogger(WebStoreAttachmentMgr.class);
    private static final String ATTACHMENT_URL = "SELECT AttachmentLocation FROM attachment WHERE AttachmentID = ";
    private static final String UNKNOWN        = "unknown";
    private static MessageDigest sha1 = null;

    
    private File                    downloadCacheDir; 
    private SimpleDateFormat        dateFormat         = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
    
    private ArrayList<AttachmentMgrListener> listeners = new ArrayList<AttachmentMgrListener>();
    private AtomicInteger                    attachCnt = new AtomicInteger(0);
    
    // URLs
    private String                  readURLStr    = null;
    private String                  writeURLStr   = null;
    private String                  delURLStr     = null;
    private String                  fileGetURLStr = null;
    private String                  fileGetMetaDataURLStr = null;
    private String                  testKeyURLStr = null;
    
    private String                  attachment_key = null;
    private String                  server_url = null;
    
    private Long                    serverTimeDelta = null;
    
    private String[]                symbols        = {"<coll>", "<disp>", "<div>", "<inst>"};
    private String[]                values  = new String[symbols.length];
    
    static
    {
        try
        {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * @throws WebStoreAttachmentException 
     * 
     */
    public WebStoreAttachmentMgr(final String urlStr, final String keyStr) throws WebStoreAttachmentException
    {
        attachment_key = keyStr == null ? "" : keyStr;
        server_url = urlStr;
                        
        downloadCacheDir = new File(UIRegistry.getAppDataDir() + File.separator + "download_cache");
        if (!downloadCacheDir.exists())
        {
            if (!downloadCacheDir.mkdir())
            {
                downloadCacheDir = null;
                throw new WebStoreAttachmentException("Failed to create download cache dir.");
            }

        } else
        {
            try
            {
                FileUtils.cleanDirectory(downloadCacheDir);
            } catch (IOException e) {}
        }
            
        getURLSetupXML(urlStr);
        testKey();
    }
    
    private void testKey() throws WebStoreAttachmentKeyException
    {
       if (testKeyURLStr == null) return; // skip test if there is not test url.
       
       GetMethod method = new GetMethod(testKeyURLStr);
       String r = "" + (new Random()).nextInt();
       method.setQueryString(new NameValuePair[] {
               new NameValuePair("random", r),
               new NameValuePair("token", generateToken(r))
       });
       
       HttpClient client = new HttpClient();
       client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
       ProxyHelper.applyProxySettings(client);

       try
       {
           int status = client.executeMethod(method);
           updateServerTimeDelta(method);
           if (status == HttpStatus.SC_OK)
           {
               return;
           } else if (status == HttpStatus.SC_FORBIDDEN)
           {
               log.error("Attachment key was not validated. HTTP status=" + status + ". Response: " + method.getResponseBodyAsString());
               throw new WebStoreAttachmentKeyException("Attachment key was not validated.");
           }
       } catch (IOException e)
       {
           // TODO Auto-generated catch block
           e.printStackTrace();
       } finally {
           method.releaseConnection();
       }
       throw new WebStoreAttachmentKeyException("Problem verifying attachment key.");
    }
    
    private void getURLSetupXML(final String urlStr) throws WebStoreAttachmentException
    {
        boolean result = false;
        if (StringUtils.isNotEmpty(urlStr))
        {
            final int timeoutMilliseconds = 5000;
            GetMethod method = new GetMethod(urlStr);
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(timeoutMilliseconds);
            client.getHttpConnectionManager().getParams().setSoTimeout(timeoutMilliseconds);
            ProxyHelper.applyProxySettings(client);

            try
            {
                int status = client.executeMethod(method);
                if (status == HttpStatus.SC_OK)
                {
                    result = getURLSFromStr(method.getResponseBodyAsString());
                    updateServerTimeDelta(method);
                }
                if (!result) {
                    log.error("Problem getting setup from URL XML. HTTP status=" + status + ". Response: " + method.getResponseBodyAsString());
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            } finally {
                method.releaseConnection();
            }
        }
        if (!result) {
            throw new WebStoreAttachmentException("Problem getting setup from URL XML.");
        }
    }
    
    private void updateServerTimeDelta(HttpMethodBase method)
    {
        try
        {
            Header timestamp = method.getResponseHeader("X-Timestamp");
            if (timestamp == null) return;
            long serverTime = Long.parseLong(timestamp.getValue());
            serverTimeDelta = serverTime - getSystemTime();
        } catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * @param webAssetFile
     * @return
     */
    private boolean getURLSFromStr(final String data)
    {
        try
        {
            Element root = XMLHelper.readStrToDOM4J(data);
            if (root != null)
            {
                for (Iterator<?> i = root.elementIterator("url"); i.hasNext();) //$NON-NLS-1$
                {
                    Element urlNode = (Element) i.next();
                    String  type    = urlNode.attributeValue("type"); //$NON-NLS-1$
                    String  urlStr  = urlNode.getTextTrim();
                    
                    if (type.equals("read"))
                    {
                        readURLStr = urlStr;
                        
                    } else if (type.equals("write"))
                    {
                        writeURLStr = urlStr;
                        
                    } else if (type.equals("delete"))
                    {
                        delURLStr = urlStr;
                        
                    } else if (type.equals("fileget"))
                    {
                        fileGetURLStr = urlStr;
                        
                    } else if (type.equals("getmetadata"))
                    {
                        fileGetMetaDataURLStr = urlStr;
                    } else if (type.equals("testkey"))
                    {
                        testKeyURLStr = urlStr;
                    }
                }
            }
            return StringUtils.isNotEmpty(readURLStr) && StringUtils.isNotEmpty(writeURLStr) && StringUtils.isNotEmpty(delURLStr);
            
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#setStorageLocationIntoAttachment(edu.ku.brc.specify.datamodel.Attachment, boolean)
     */
    @Override
    public boolean setStorageLocationIntoAttachment(Attachment attachment, boolean doDisplayErrors)
    {
        //Possibly for unsaved attachments, origianalFilename could be null? 
    	//It is happening for KU Herps anyway, so adding check for null origFilename here... 
    	String origExtension = attachment.getOrigFilename() == null ? "" :
        	attachment.getOrigFilename().substring(attachment.getOrigFilename().lastIndexOf('.'));
        String suffix     = ".att";
        
        if (!"".equals(origExtension))
        {
            // Make sure the file extension (if any) remains the same so the host
            // filesystem still sees the files as the proper types.  This is simply
            // to make the files browsable from a system file browser.
            suffix = ".att" + origExtension;
        }
        
        String errMsg          = null;
        String storageFilename = "";
        try
        {
            File storageFile = File.createTempFile("sp6", suffix, downloadCacheDir.getAbsoluteFile());
            //System.err.println("["+storageFile.getAbsolutePath()+"] "+storageFile.canWrite());
            if (storageFile.exists())
            {
                attachment.setAttachmentLocation(storageFile.getName());
                
                // This is kludgie as hell, but delete the cache file so that it will be downloaded
                // after the attachment is stored. Otherwise, the system will think the empty file
                // is the cached attachment file.
                storageFile.delete();
                
                return true;
            }
            errMsg = UIRegistry.getLocalizedMessage("ATTCH_NOT_SAVED_REPOS", storageFile.getAbsolutePath());
            log.error("storageFile doesn't exist["+storageFile.getAbsolutePath()+"]");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            
            if (doDisplayErrors)
            {
                errMsg = UIRegistry.getLocalizedMessage("ATTCH_NOT_SAVED_REPOS", storageFilename);
            } else
            {
                // This happens when errors are not displayed.
                e.printStackTrace();
                edu.ku.brc.af.core.UsageTracker.incrHandledUsageCount();
                edu.ku.brc.exceptions.ExceptionTracker.getInstance().capture(FileStoreAttachmentManager.class, e);
            }
        }
        
        if (doDisplayErrors && errMsg != null)
        {
            UIRegistry.showError(errMsg);
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getFileEmbddedDate(int)
     */
    @Override
    public Calendar getFileEmbeddedDate(final int attachmentID)
    {
        String dateStr = null;
        String fileName = BasicSQLUtils.querySingleObj(ATTACHMENT_URL + attachmentID);
        if (StringUtils.isNotEmpty(fileName) && StringUtils.isNotEmpty(fileGetMetaDataURLStr))
        {
            GetMethod method = new GetMethod(fileGetMetaDataURLStr);
            fillValuesArray();
            method.setQueryString(new NameValuePair[] {
                    new NameValuePair("dt", "json"),
                    new NameValuePair("filename", fileName),
                    new NameValuePair("token", generateToken(fileName)),
                    new NameValuePair("coll", values[0]),
                    new NameValuePair("disp", values[1]),
                    new NameValuePair("div",  values[2]),
                    new NameValuePair("inst", values[3])
            });
    
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            ProxyHelper.applyProxySettings(client);

            try
            {
                int status = client.executeMethod(method);
                updateServerTimeDelta(method);
                if (status == HttpStatus.SC_OK)
                {
                    dateStr= method.getResponseBodyAsString();
                } else {
                    log.warn("Http status: " + status);
                }
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                method.releaseConnection();
            }
        }     
                
        if (dateStr != null && dateStr.length() == 10)
        {
            try
            {
                Date convertedDate = dateFormat.parse(dateStr);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(convertedDate.getTime());
                return cal;                        
            } catch (ParseException e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getMetaDataAsJSON(int)
     */
    @Override
    public String getMetaDataAsJSON(final int attachmentID)
    {
      String result = null;
      String fileName = BasicSQLUtils.querySingleObj(ATTACHMENT_URL + attachmentID);
      if (StringUtils.isNotEmpty(fileName) && StringUtils.isNotEmpty(fileGetMetaDataURLStr))
      {
            GetMethod method = new GetMethod(fileGetMetaDataURLStr);
            fillValuesArray();
            method.setQueryString(new NameValuePair[] {
                    new NameValuePair("dt", "json"),
                    new NameValuePair("filename", fileName),
                    new NameValuePair("token", generateToken(fileName)),
                    new NameValuePair("coll", values[0]),
                    new NameValuePair("disp", values[1]),
                    new NameValuePair("div",  values[2]),
                    new NameValuePair("inst", values[3])
            });
    
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            ProxyHelper.applyProxySettings(client);

            try
            {
                int status = client.executeMethod(method);
                updateServerTimeDelta(method);
                if (status == HttpStatus.SC_OK)
                {
                    result = method.getResponseBodyAsString();
                } else {
                    log.warn("Http status: " + status);
                }
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                method.releaseConnection();
            }
      }     
      return result;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getOriginal(edu.ku.brc.specify.datamodel.Attachment)
     */
    @Override
    public synchronized File getOriginal(final Attachment attachment, final byte[] bytes)
    {
        return getOriginal(attachment.getAttachmentLocation(), attachment.getOrigFilename(), attachment.getMimeType(), bytes);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getOriginal(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public File getOriginal(final String attachLoc, final String originalLoc, final String mimeType, final byte[] bytes)
    {
        return getFile(attachLoc, originalLoc, mimeType, null, bytes);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getOriginalScaled(java.lang.String, java.lang.String, java.lang.String, int)
     */
    @Override
    public File getOriginalScaled(final String attachLoc,
                                  final String originalLoc,
                                  final String mimeType,
                                  final int maxSideInPixels,
                                  final byte[] bytes)
    {
        return getFile(attachLoc, originalLoc, mimeType, maxSideInPixels, bytes);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getThumbnail(edu.ku.brc.specify.datamodel.Attachment, int)
     */
    @Override
    public synchronized File getThumbnail(final Attachment attachment, final int maxSideInPixels)
    {
        return getOriginalScaled(attachment.getAttachmentLocation(), attachment.getOrigFilename(), attachment.getMimeType(), maxSideInPixels, new byte[10240]);
    }

    /**
     * @param fileName
     * @param doDelOnExit
     * @return a file that is in the cache directory but will be deleted on exit
     * @throws IOException 
     */
    private File createTempFile(final String fileName, final boolean doDelOnExit) throws IOException
    {
        String fileExt = FilenameUtils.getExtension(fileName);
        File file = File.createTempFile("sp6", '.' + fileExt, downloadCacheDir.getAbsoluteFile());
        if (doDelOnExit)
        {
            file.deleteOnExit();
        }
        return file;
    }
    
    private File getDLFileForName(final String fileName)
    {
        return new File(downloadCacheDir.getAbsoluteFile(), fileName);
    }

    // May God forgive me for this horrible kludge.
    private File getFileForIconName(final String iconName)
    {
        IconEntry entry = IconManager.getIconEntryByName(iconName);
        if (entry != null)
        {
            try
            {
                //System.err.println("****** entry.getUrl(): "+entry.getUrl().toExternalForm());
                
                String fullPath = entry.getUrl().toExternalForm();
                if (fullPath.startsWith("jar:"))
                {
                    String[] segs = StringUtils.split(fullPath, "!");
                    if (segs.length != 2) return null;
                    
                    String jarPath  = segs[1];
                    InputStream stream = IconManager.class.getResourceAsStream(jarPath);
                    if (stream == null) {
                        //send your exception or warning
                        return null;
                    }
                    
                    String fileName = FilenameUtils.getName(jarPath);
                    File   outfile  = new File(downloadCacheDir, fileName);
                    //System.err.println("Path: "+ path+"|"+jarPath+"|"+fileName);
                    OutputStream resStreamOut;
                    int          readBytes;
                    byte[] buffer =  new byte[10240];
                    try {
                        resStreamOut = new FileOutputStream(outfile);
                        while ((readBytes = stream.read(buffer)) > 0) 
                        {
                            resStreamOut.write(buffer, 0, readBytes);
                        }
                        resStreamOut.close();
                        stream.close();
                        
                        return outfile;
                        
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        return null;
                    }
                } else {
                    
                    return new File(entry.getUrl().toURI());
                }
            } catch (URISyntaxException e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /**
     * @param srcFile
     * @param destFileName
     * @param mimeType
     * @param scale
     * @return
     */
    private File getThumnailFromFile(final File    srcFile,
                                     final String  mimeType,
                                     final Integer scale)
    {
        try
        {
            File   tmpFile = createTempFile(srcFile.getName(), true); // gets deleted automatically

            // Now generate the thumbnail 
            Thumbnailer thumbnailGen  = AttachmentUtils.getThumbnailer();
            thumbnailGen.generateThumbnail(srcFile.getAbsolutePath(), 
                                           tmpFile.getAbsolutePath(),
                                           false);
            

            return tmpFile;
            
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return getFileForIconName(UNKNOWN);
    }
    
 
    
    /**
     * Returns true if the Attachment Server can create an image for it.
     * (Ideally this class should as the server for the mime types)
     * @param mimeType the mime type of the file
     * @return true if it is retruned as an image
     */
    private boolean isAvailableAsImage(final String  mimeType)
    {
        return mimeType.startsWith("image/") ||
               mimeType.endsWith("pdf");
    }


    /**
     * @param attachLocation
     * @param originalLoc
     * @param mimeTypeArg
     * @param scale
     * @return
     */
    private File getFile(final String  attachLocation,
                         final String  originalLoc,
                         final String  mimeTypeArg,
                         final Integer scale,
                         final byte[]  bytes)
    {
        // Check to see what locations were passed in
        boolean hasAttachmentLoc = StringUtils.isNotEmpty(attachLocation);
        boolean hasOrigFileName  = StringUtils.isNotEmpty(originalLoc);
        boolean hasScaleSize     = scale != null;
        
        if (!hasAttachmentLoc && !hasOrigFileName) return getFileForIconName(UNKNOWN);
        
        String  mimeType = mimeTypeArg != null ? mimeTypeArg : AttachmentUtils.getMimeType(hasAttachmentLoc ? attachLocation : originalLoc);
        boolean isImage  = isAvailableAsImage(mimeType);
        
        String fileNameToGet = attachLocation;
        if (hasAttachmentLoc)
        {
            //////////////////////////////////////////////////////////
            // If scale is not null, then it contains the scale size
            // so change the name to a scale name
            //////////////////////////////////////////////////////////
            if (hasScaleSize)
            {   
                fileNameToGet = getScaledFileName(attachLocation, scale);
            }
            
            
            File dlFile = getDLFileForName(fileNameToGet);
            if (dlFile.exists()) {
                return dlFile;
            }
            
        }
        
        //////////////////////////////////////////////////////////////////////
        // If we are here then the server side filename is not in the cache
        // for the full file or scaled file and the original file is not
        // not in the cache.
        //
        // If the 'hasAttachmentLoc' is null then it isn't saved on the Server 
        // yet and we need to either get the full file from disk or generate 
        // a thumbnail.
        //////////////////////////////////////////////////////////////////////
        
        // Now let's get the full file, it's a local file
        // hopefully it is still there
        if (!hasAttachmentLoc)
        {
            File fullFile = new File(originalLoc);
            
            // Ok, it isn't on the server yet.
            if (fullFile.exists())
            {
                return hasScaleSize ? getThumnailFromFile(fullFile, mimeType, scale) : fullFile;
            }
            return getFileForIconName(UNKNOWN);
        }
        
        // The File is on the server.
        //
        // Now if we need a scaled version of the file we need to make
        // sure we have a thumbnailer that can make the scaled image,
        // if we don't then just get an icon.
        
        if (hasScaleSize)
        {   
            if (isImage) // Images get scaled on the server
            {   
                return getFileFromWeb(attachLocation, mimeType, scale, bytes); // ask server for scaled image
            } else {
                return getIconFromFileName(attachLocation);
            }
        }
        
        // Get the Full Image
        // It's not an image, so we need to get the whole file
        return getFileFromWeb(attachLocation, mimeType, null, bytes);
    }
        
    private File getIconFromFileName(final String fileName)
    {
        String iconName = Thumbnailer.getIconNameFromExtension(FilenameUtils.getExtension(fileName.toLowerCase()));
        if (iconName != null)
        {
            File iconFile = getFileForIconName(iconName);
            if (iconFile != null) 
            { 
                return iconFile;
            }
        }
        // No thumbnail, no icon, use the 'unknown' icon
        return getFileForIconName(UNKNOWN);
    }
    
    /**
     * @param inc
     */
    private synchronized void notifyListeners(final int inc) 
    {
        attachCnt.set(attachCnt.intValue() + inc);
        final int count = attachCnt.intValue();
        
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                for (AttachmentMgrListener l : listeners)
                {
                    l.filesLoading(count);
                }
            }
        });
    }
    
    /**
     * @param urlStr
     * @param tmpFile
     * @return
     * @throws IOException
     */

    private File getFileFromWeb(final String attachLocation, 
                                 final String mimeType, 
                                 final Integer scale,
                                 final byte[] bytes)
    {
        File dlFile = getDLFileForName((scale == null) ? attachLocation: getScaledFileName(attachLocation, scale));
        try
        {
            dlFile.createNewFile();
            dlFile.deleteOnExit();
        } catch (IOException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return null;
        }

        
        boolean success = false;
       
        if (bytes == null)
        {
            System.out.println("bytes == null");
        }
        notifyListeners(1);

        GetMethod getMethod = new GetMethod(readURLStr);
        // type=<type>&filename=<fname>&coll=<coll>&disp=<disp>&div=<div>&inst=<inst>
        fillValuesArray();
        getMethod.setQueryString(new NameValuePair[] {
                new NameValuePair("type", (scale != null) ? "T" : "O"),
                new NameValuePair("scale", "" + scale),
                new NameValuePair("filename", attachLocation),
                new NameValuePair("token", generateToken(attachLocation)),
                new NameValuePair("coll", values[0]),
                new NameValuePair("disp", values[1]),
                new NameValuePair("div",  values[2]),
                new NameValuePair("inst", values[3])
        });
                         
                
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        ProxyHelper.applyProxySettings(client);

        try
        {
            int status = client.executeMethod(getMethod);
            updateServerTimeDelta(getMethod);
            if (status == HttpStatus.SC_OK)
            {
                InputStream inpStream = getMethod.getResponseBodyAsStream();
                if (inpStream != null)
                {
                    BufferedInputStream  in  = new BufferedInputStream(inpStream);
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dlFile));
                    
                    //long totBytes = 0;
                    do
                    {
                        int numBytes = in.read(bytes);
                        if (numBytes == -1)
                        {
                            break;
                        }
                        //totBytes += numBytes;
                        bos.write(bytes, 0, numBytes);
                        
                    } while(true);
                    //log.debug(String.format("Total Bytes for file: %d %d", totBytes, totBytes / 1024));
                    in.close();
                    bos.flush();
                    bos.close();
                
                    success = true;
                }
            } else {
                log.error("HTTP status: " + status + ". Response: " + getMethod.getResponseBodyAsString());
            }
        } catch (HttpException e)
        {
            e.printStackTrace();
            log.error(e);
        } catch (IOException e)
        {
            e.printStackTrace();
            log.error(e);
        } finally 
        {
            getMethod.releaseConnection();
            notifyListeners(-1);
        }
    
        if (success) {
            return dlFile;
        } else{
            dlFile.delete();
            return null;
        }
    }
    
    private Long getSystemTime()
    {
        return System.currentTimeMillis() / 1000L + 100000;
    }
    
    private String generateToken(String attachLocation)
    {
        if (StringUtils.isEmpty(attachment_key)) return "";
        
        SecretKeySpec keySpec = new SecretKeySpec(attachment_key.getBytes(), "HmacMD5");
        Mac mac;
        try
        {
            mac = Mac.getInstance("HmacMD5");
            mac.init(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeyException e)
        {
            throw new RuntimeException(e);
        }
        
        String timestamp = "" + (getSystemTime() + serverTimeDelta);
        byte[] raw = mac.doFinal((timestamp + attachLocation).getBytes());
                
        return  new String(Hex.encodeHex(raw)) + ":" + timestamp;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#storeAttachmentFile(edu.ku.brc.specify.datamodel.Attachment, java.io.File, java.io.File)
     */
    @Override
    public void storeAttachmentFile(final Attachment attachment, final File attachmentFile, final File thumbnail) throws IOException
    {
        
        if (sendFile(attachmentFile, attachment.getAttachmentLocation(), false))
        {
            sendFile(thumbnail, attachment.getAttachmentLocation(), true);
        } else
        {
            throw new IOException(String.format("File [%s] was not saved on the server!", attachmentFile.getName()));
        }
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#setThumbSize(int)
     */
    @Override
    public void setThumbSize(int sizeInPixels)
    {
        
    }

 
    /**
     * 
     */
    private void fillValuesArray()
    {
        Collection  coll = AppContextMgr.getInstance().getClassObject(Collection.class);
        Discipline  disp = AppContextMgr.getInstance().getClassObject(Discipline.class);
        Division    div  = AppContextMgr.getInstance().getClassObject(Division.class);
        Institution inst = AppContextMgr.getInstance().getClassObject(Institution.class);
 
        values[0] = coll.getCollectionName();
        values[1] = disp.getName();
        values[2] = div.getName();
        values[3] = inst.getName();
    }
    
    /**
     * @param targetFile
     * @param fileName
     * @param isThumb
     * @return
     */
    private synchronized boolean sendFile(final File targetFile, 
                                          final String fileName, 
                                          final boolean isThumb)/*,
                                          final boolean saveInCache)*/
    {
        String targetURL = writeURLStr;
        PostMethod filePost = new PostMethod(targetURL);

        fillValuesArray();
        
        try
        {
            log.debug("Uploading " + targetFile.getName() + " to " + targetURL);

            Part[] parts = {
                    new FilePart(targetFile.getName(), targetFile),
                    new StringPart("type", isThumb ? "T" : "O"),
                    new StringPart("store", fileName),
                    new StringPart("token", generateToken(fileName)),
                    new StringPart("coll", values[0]),
                    new StringPart("disp", values[1]),
                    new StringPart("div",  values[2]),
                    new StringPart("inst", values[3]),
                };

            filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            ProxyHelper.applyProxySettings(client);

            int status = client.executeMethod(filePost);
            updateServerTimeDelta(filePost);
            
            //log.debug("---------------------------------------------------");
            log.debug(filePost.getResponseBodyAsString());
            //log.debug("---------------------------------------------------");

            if (status == HttpStatus.SC_OK)
            {
                return true;
            } else {
                log.warn("Http status: " + status + ". Response: " + filePost.getResponseBodyAsString());
            }
            
        } catch (Exception ex)
        {
            log.error("Error:  " + ex.getMessage());
            ex.printStackTrace();
            
        } finally
        {
            filePost.releaseConnection();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#replaceOriginal(edu.ku.brc.specify.datamodel.Attachment, java.io.File, java.io.File)
     */
    @Override
    public void replaceOriginal(Attachment attachment, File newOriginal, File newThumbnail) throws IOException
    {
        
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#deleteAttachmentFiles(edu.ku.brc.specify.datamodel.Attachment)
     */
    @Override
    public void deleteAttachmentFiles(final Attachment attachment) throws IOException
    {
        String targetFileName = attachment.getAttachmentLocation();
        if (deleteFileFromWeb(targetFileName, false))
        {
            // the server deletes thumbs when deleting original.
//            deleteFileFromWeb(targetFileName, true); // ok to fail deleting thumb
        } else
        {
            UIRegistry.showLocalizedError("ATTCH_NOT_DEL_REPOS", targetFileName);
        }
    }
    
    /**
     * @param fileName
     * @param scale
     * @return
     */
    private String getScaledFileName(final String fileName, final Integer scale)
    {
        String newPath = FilenameUtils.removeExtension(fileName);
        String ext     = FilenameUtils.getExtension(fileName);
        return String.format("%s_%d%s%s", newPath, scale, FilenameUtils.EXTENSION_SEPARATOR_STR, ext);
    }
    
    /**
     * @param fileName
     * @param isThumb
     * @return
     */
    private boolean deleteFileFromWeb(final String fileName, final boolean isThumb)
    {
        try
        {
            //String     targetURL  = String.format("http://localhost/cgi-bin/filedelete.php?filename=%s;disp=%s", targetName, discipline.getName());
            //String     targetURL  = subAllExtraData(delURLStr, fileName, isThumb, null, null);
            fillValuesArray();
            PostMethod  postMethod  = new PostMethod(delURLStr);
            postMethod.addParameter("filename", fileName);
            postMethod.addParameter("token", generateToken(fileName));
            postMethod.addParameter("coll", values[0]);
            postMethod.addParameter("disp", values[1]);
            postMethod.addParameter("div",  values[2]);
            postMethod.addParameter("inst", values[3]);            
            //log.debug("Deleting " + fileName + " from " + targetURL );

            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            ProxyHelper.applyProxySettings(client);

            int status = client.executeMethod(postMethod);
            updateServerTimeDelta(postMethod);
            
            if (status != HttpStatus.SC_OK) {
                log.warn("HttpStatus=" + status + ". Response: " + postMethod.getResponseBodyAsString());
            }


            return status == HttpStatus.SC_OK || status == HttpStatus.SC_NOT_FOUND;
            
        } catch (Exception ex)
        {
            log.error(ex);
            ex.printStackTrace();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#regenerateThumbnail(edu.ku.brc.specify.datamodel.Attachment)
     */
    @Override
    public File regenerateThumbnail(final Attachment attachment) throws IOException
    {
        File thumbFile = null;
        
        boolean doLocalFile = false;
        
        String origFilePath = attachment.getAttachmentLocation();
        if (StringUtils.isEmpty(origFilePath))
        {
            doLocalFile = true;
            origFilePath = attachment.getOrigFilename();
            if (StringUtils.isEmpty(origFilePath))
            {
                return null;
            }
        }
        
        File origFile;
        if (doLocalFile)
        {
            origFile = new File(origFilePath);
            
        } else
        {
            origFile = getOriginal(attachment, new byte[10240]);
        }
        
        if (origFile != null)
        {
            thumbFile = createTempFile(origFile.getName(), true);
            
            Thumbnailer thumbnailGen   = AttachmentUtils.getThumbnailer();
            thumbnailGen.generateThumbnail(origFile.getAbsolutePath(), 
                                           thumbFile.getAbsolutePath(),
                                           false);
            if (thumbFile.exists())
            {
                if (!doLocalFile)
                {
                    sendFile(thumbFile, thumbFile.getName(), true);
                }
                
            }
        }
        return thumbFile;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#addListener(edu.ku.brc.util.AttachmentMgrListener)
     */
    @Override
    public void addListener(AttachmentMgrListener listener)
    {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#removeListener(edu.ku.brc.util.AttachmentMgrListener)
     */
    @Override
    public void removeListener(AttachmentMgrListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * @param algorithm
     * @param fileName
     * @return
     * @throws Exception
     */
    private String calculateHash(final File file) throws Exception
    {
        if (sha1 != null)
        {
            FileInputStream     fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DigestInputStream   dis = new DigestInputStream(bis, sha1);
    
            // read the file and update the hash calculation
            while (dis.read() != -1)
                ;
    
            // get the hash value as byte array
            byte[] hash = sha1.digest();

            dis.close();
            return byteArray2Hex(hash);
        }
        return null;
    }

    /**
     * @param hash
     * @return
     */
    private String byteArray2Hex(byte[] hash)
    {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String s = formatter.toString();
        formatter.close();
        return s;
    }


    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#setDirectory(java.io.File)
     */
    @Override
    public void setDirectory(final File baseDir) throws IOException
    {
        
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getDirectory()
     */
    @Override
    public File getDirectory()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#isDiskBased()
     */
    @Override
    public boolean isDiskBased()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#getImageAttachmentURL()
     */
    @Override
    public String getImageAttachmentURL()
    {
        return fileGetURLStr;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.util.AttachmentManagerIface#cleanup()
     */
    @Override
    public void cleanup()
    {

    }

    /**
     * @return the readURLStr
     */
    public String getReadURLStr()
    {
        return readURLStr;
    }

    /**
     * @return the delURLStr
     */
    public String getDelURLStr()
    {
        return delURLStr;
    }

    /**
     * @return the fileGetURLStr
     */
    public String getFileGetURLStr()
    {
        return fileGetURLStr;
    }

    /**
     * @return the fileGetMetaDataURLStr
     */
    public String getFileGetMetaDataURLStr()
    {
        return fileGetMetaDataURLStr;
    }

    public String getServerURL()
    {
        return server_url;
    }
    
    class FileDownloadQueue {
        String url;
        File   tmpFile;
        /**
         * @param url
         * @param tmpFile
         */
        public FileDownloadQueue(String url, File tmpFile)
        {
            super();
            this.url = url;
            this.tmpFile = tmpFile;
        }
    }
}
