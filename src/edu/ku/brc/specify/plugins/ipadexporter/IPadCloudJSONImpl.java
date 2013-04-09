/* Copyright (C) 2012, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
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
package edu.ku.brc.specify.plugins.ipadexporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.lang.StringUtils;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Sep 19, 2012
 *
 */
public class IPadCloudJSONImpl implements IPadCloudIFace
{
    // URLs
    private String writeURLStr = iPadRepositoryHelper.baseURLStr + "spinsight.php";
    
    private static final String kId          = "id";
    private static final String kIsGlobal    = "isglob";
    private static final String kCollection  = "col";
    private static final String kDiscipline  = "dsp";
    private static final String kDivision    = "div";
    //private static final String kInstitution = "inst";
    private static final String kDataSetName = "dsname";
    private static final String kUserName    = "usrname";
    private static final String kPassword    = "pwd";
    private static final String kDirName     = "dirname";
    private static final String kCurator    = "curator";
    private static final String kIcon        = "icon";
    private static final String kAction      = "action";
    
    // For Institution
    private static final String kWebSite     = "website";
    private static final String kTitle       = "title";
    private static final String kCode        = "code";
    private static final String kGUID        = "guid";
    
    // Data Members
    private boolean                 isLoggedIn = false;
    private Integer                 currUserID = null;
    private String                  userName   = null;
    

    /**
     * 
     */
    public IPadCloudJSONImpl()
    {
        super();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#isLoggedIn()
     */
    @Override
    public boolean isLoggedIn()
    {
        return isLoggedIn && currUserID != null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#isUserNameOK(java.lang.String)
     */
    @Override
    public boolean isUserNameOK(final String usrName)
    {
        HashMap<String, String> map = createHashMap(kUserName, usrName, kAction, "isnameok");
        JSONObject data = sendPost(map);
        return data != null && isStatusOK(data);
    }
    
    /**
     * @param data
     * @return
     */
    private boolean isStatusOK(final JSONObject data)
    {
        String statusStr = ((JSONObject) data).getString("status");
        return StringUtils.isNotEmpty(statusStr) && statusStr.equals("OK");
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#login(java.lang.String, java.lang.String)
     */
    @Override
    public boolean login(final String usrName, final String pwd)
    {
        logout();
        
        String md5Pwd = org.apache.commons.codec.digest.DigestUtils.md5Hex(pwd);
        
        //org.apache.commons.codec.digest.DigestUtils.md5Hex(data)
        System.out.println(String.format("[%s][%s]", pwd, md5Pwd));
        
        HashMap<String, String> map = createHashMap(kUserName, usrName, kPassword, md5Pwd, kAction, "login");
        JSONObject data = sendPost(map);
        
        if (data != null && isStatusOK(data))
        {
            String idStr = ((JSONObject)data).getString("id");
            currUserID = StringUtils.isNotEmpty(idStr) ? Integer.parseInt(idStr) : null;
            if (currUserID != null)
            {
                isLoggedIn = true;
                userName   = usrName;
            }
        }
        return isLoggedIn;
    }
    
    /*JSONArray indices = data.getJSONArray("indices");
    Map<String, String> result = new HashMap<String, String>();
    for (Object index : indices)
    {
        String name = ((JSONObject) index).getString("name");
        String url = ((JSONObject) index).getString("url");
        result.put(name, url);
    }*/

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#logout()
     */
    @Override
    public boolean logout()
    {
        isLoggedIn = false;
        currUserID = null;
        userName   = null;
        return true;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#addUserAccessToDataSet(java.lang.String, java.lang.String)
     */
    @Override
    public boolean addUserAccessToDataSet(final String usrName, final String dataSetName)
    {
        HashMap<String, String> map = createHashMap(kUserName, usrName, kDataSetName, dataSetName, kAction, "addaccess");
        JSONObject data = sendPost(map);
        
        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#addDataSetToUser(java.lang.String, java.lang.String)
     */
    @Override
    public boolean addDataSetToUser(final String usrName, final String dataSetName)
    {
        HashMap<String, String> map = createHashMap(kUserName, usrName, kDataSetName, dataSetName, kAction, "addowner");
        JSONObject data = sendPost(map);
        
        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#doesDataSetExist(java.lang.String, java.lang.String)
     */
    @Override
    public boolean doesDataSetExist(final String dsName, final String guid)
    {
        HashMap<String, String> map = createHashMap(kDataSetName, dsName, kGUID, guid, kAction, "dsexists");
        JSONObject data = sendPost(map);
        
        if (data != null && isStatusOK(data))
        {
            String boolStr = ((JSONObject) data).getString("exists");
            return StringUtils.isNotEmpty(boolStr) && boolStr.equals("true");
        }
        return false;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#removeUserAccessFromDataSet(java.lang.String, java.lang.String)
     */
    @Override
    public boolean removeUserAccessFromDataSet(final String usrName, final String dataSetName)
    {
        HashMap<String, String> map = createHashMap(kUserName, usrName, kDataSetName, dataSetName, kAction, "delaccess");
        JSONObject data = sendPost(map);
        
        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#removeDataSetFromUser(java.lang.String, java.lang.String)
     */
    @Override
    public boolean removeDataSetFromUser(final String usrName, final String dataSetName)
    {
        HashMap<String, String> map = createHashMap(kUserName, usrName, kDataSetName, dataSetName, kAction, "delowner");
        JSONObject data = sendPost(map);
        
        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#addNewDataSet(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, java.lang.String, java.lang.String)
     */
    @Override
    public boolean addNewDataSet(final String dsName,
                                 final String dirName,
                                 final String guid,
                                 final String div,
                                 final String disp,
                                 final String coll,
                                 final Boolean isGlobal,
                                 final String iconName,
                                 final String curator)
    {
        HashMap<String, String> map = createHashMap(kUserName,    userName,
                                                    kCollection,  coll, 
                                                    kDiscipline,  disp, 
                                                    kDivision,    div, 
                                                    kGUID,        guid, 
                                                    kDirName,     dirName, 
                                                    kIsGlobal,    isGlobal != null ? isGlobal.toString() : "false", 
                                                    kIcon,        iconName, 
                                                    kCurator,     curator, 
                                                    kAction,      "adddataset");
        JSONObject data = sendPost(map);
        
        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#removeDataSet(java.lang.String, java.lang.String)
     */
    @Override
    public boolean removeDataSet(final String dsName, final String guid)
    {
        HashMap<String, String> map = createHashMap(
                kGUID,     guid, 
                kDataSetName, dsName, 
                kAction,      "deldataset");
        JSONObject data = sendPost(map);

        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#addNewUser(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean addNewUser(final String usrName, final String pwd, final String guid)
    {
        HashMap<String, String> map = createHashMap(kUserName, usrName, kPassword, pwd, kGUID, guid, kAction, "adduser");
        JSONObject data = sendPost(map);
        
        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#getAccessList(java.lang.String)
     */
    @Override
    public List<String> getAccessList(final String dataSetName)
    {
        HashMap<String, String> map = createHashMap(kDataSetName, dataSetName, kAction, "getaccesslist");
        JSONObject data = sendPost(map);
        
        if (data != null && isStatusOK(data))
        {
            JSONArray ids = data.getJSONArray("ids");
            ArrayList<String> idList = new ArrayList<String>();
            for (Object id : ids)
            {
                idList.add(id.toString());
            }
            return idList;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#makeDataSetGlobal(java.lang.String, boolean)
     */
    @Override
    public boolean makeDataSetGlobal(final String dataSetName, final boolean isGlobal)
    {
        HashMap<String, String> map = createHashMap(kDataSetName, dataSetName, kIsGlobal, isGlobal ? "0" : "1", kAction, "setglobal");
        JSONObject data = sendPost(map);
        
        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#removeAccount()
     */
    @Override
    public boolean removeAccount()
    {
        HashMap<String, String> map = createHashMap(kId, currUserID.toString(), kAction, "deluser");
        JSONObject data = sendPost(map);
        
        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#setPassword(java.lang.String)
     */
    @Override
    public boolean setPassword(final String newPwd)
    {
        HashMap<String, String> map = createHashMap(kUserName, userName, kAction, "setpwd");
        JSONObject data = sendPost(map);
        
        return data != null && isStatusOK(data);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#sendPwdReminder()
     */
    @Override
    public boolean sendPwdReminder()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#getOwnerList()
     */
    @Override
    public Vector<DataSetInfo> getOwnerList()
    {
        HashMap<String, String> map = createHashMap(kId, currUserID.toString(), kAction, "getownerlist", kUserName, userName);
        JSONObject data = sendPost(map);
        
        if (data != null && isStatusOK(data))
        {
            Vector<DataSetInfo> dsList = new Vector<DataSetInfo>();
            JSONArray datasets = data.getJSONArray("datasets");
            for (Object ds : datasets)
            {
                if (ds instanceof JSONObject)
                {
                    JSONObject jds = (JSONObject)ds;
                    String idStr  = jds.getString("id");
                    String dsname = jds.getString("dsname");
                    String inst   = jds.getString("inst");
                    String div    = jds.getString("div");
                    String dsp    = jds.getString("dsp");
                    String col    = jds.getString("col");
                    String isglob = jds.getString("isglob");
                    Integer id = Integer.parseInt(idStr);
                    DataSetInfo dsi = new DataSetInfo(id, dsname, inst, div, dsp, col, isglob.equals("1"));
                    dsList.add(dsi);
                }
            }
            return dsList;
        }
        return null;
    }
    
    /**
     * @param items
     * @return
     */
    private HashMap<String, String> createHashMap(String...items)
    {
        if (items.length % 2 == 0)
        {
            HashMap<String, String> map = new HashMap<String, String>();
            for (int i=0;i< items.length;i++)
            {
                map.put(items[i], items[i+1]);
                i++;
            }
            return map;
        }
        return null;
    }
    
    /**
     * @param valuesMap
     * @return
     */
    private synchronized JSONObject sendPost(final HashMap<String, String> valuesMap)
    {
        System.out.println("\n------------------------ ");
        for (String k : valuesMap.keySet())
        {
            System.out.println(String.format("[%s] [%s]", k, valuesMap.get(k)));
        }
        System.out.println("------------------------\n"+writeURLStr);
        PostMethod post   = new PostMethod(writeURLStr);
        try
        {
            Part[] parts = new Part[valuesMap.size()];
            int i = 0;
            for (String key : valuesMap.keySet())
            {
                parts[i++] = new StringPart(key, valuesMap.get(key));
            }

            post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);

            int status = client.executeMethod(post);
            
            if (status == HttpStatus.SC_OK)
            {
                System.err.println("HTTP Status: OK");
                String outStr = post.getResponseBodyAsString();
                System.out.println("["+outStr+"]");
                
                return JSONObject.fromObject(outStr);
            }
            
            System.err.println("HTTP Status: "+status);
            System.err.println(post.getResponseBodyAsString());
            
        } catch (java.net.UnknownHostException uex)
        {
            //networkConnError = true;
            
        } catch (Exception ex)
        {
            System.out.println("Error:  " + ex.getMessage());
            ex.printStackTrace();
            
        } finally
        {
            post.releaseConnection();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#getInstList()
     */
    @Override
    public List<String> getInstList()
    {
        HashMap<String, String> map = createHashMap(kAction, "getinstlist");
        JSONObject data = sendPost(map);
        if (data != null && isStatusOK(data))
        {
            ArrayList<String> items = new ArrayList<String>();
            JSONArray instNames = (JSONArray)data.getJSONArray("insts");
            for (Object nm : instNames)
            {
                items.add(nm.toString());
            }
            return items;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#getInstId(java.lang.String)
     */
    @Override
    public Integer getInstId(final String guid)
    {
        HashMap<String, String> map = createHashMap(kGUID, guid, kAction, "getinstid");
        JSONObject data = sendPost(map);
        
        if (data != null && isStatusOK(data))
        {
            return data.getInt("id");
        }
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#getInstitutionInfo(int)
     */
    @Override
    public HashMap<String, Object> getInstitutionInfo(int instId)
    {
        HashMap<String, String> map    = createHashMap(kId, Integer.toString(instId), kAction, "getinst");
        JSONObject data = sendPost(map);
        
        if (data != null && isStatusOK(data))
        {
            JSONObject instObj = data.getJSONObject("inst");
            if (instObj != null)
            {
                HashMap<String, Object> values = new HashMap<String, Object>();
                String idStr  = instObj.getString("id");
                values.put("Id",      Integer.parseInt(idStr));
                values.put("Title",   instObj.getString("title"));
                values.put("WebSite", instObj.getString("website"));
                values.put("Code",    instObj.getString("code"));
                values.put("GUID",    instObj.getString("guid"));
                return values;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.ipadexporter.IPadCloudIFace#saveInstitutionInfo(java.lang.Integer, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Integer saveInstitutionInfo(Integer instId, String title, String webSite, String code, final String guid)
    {
        HashMap<String, String> map = createHashMap(kTitle, title, kWebSite, webSite, kCode, code, kAction, "addinst");
        if (instId != null)
        {
            map.put(kId, instId.toString());
        }
        
        JSONObject data = sendPost(map);
        
        if (data != null && isStatusOK(data))
        {
            if (instId == null)
            {
                Integer newInstId = data.getInt("newid");
                if (newInstId != null)
                {
                    return newInstId;
                }
            } else
            {
                return instId;
            }
        }
        return null;
    }
}