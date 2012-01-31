/* Copyright (C) 2009, University of Kansas Center for Research
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
package edu.ku.brc.specify.config;

import static edu.ku.brc.specify.conversion.BasicSQLUtils.getCount;
import static edu.ku.brc.specify.conversion.BasicSQLUtils.getCountAsInt;
import static edu.ku.brc.specify.conversion.BasicSQLUtils.query;
import static edu.ku.brc.specify.conversion.BasicSQLUtils.update;
import static edu.ku.brc.ui.UIRegistry.getLocalizedMessage;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JEditorPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.ui.forms.persist.AltViewIFace;
import edu.ku.brc.af.ui.forms.persist.FormCellFieldIFace;
import edu.ku.brc.af.ui.forms.persist.FormCellIFace;
import edu.ku.brc.af.ui.forms.persist.FormRowIFace;
import edu.ku.brc.af.ui.forms.persist.FormViewDef;
import edu.ku.brc.af.ui.forms.persist.FormViewDefIFace;
import edu.ku.brc.af.ui.forms.persist.ViewDefIFace;
import edu.ku.brc.af.ui.forms.persist.ViewIFace;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.AccessionAttachment;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.AgentAttachment;
import edu.ku.brc.specify.datamodel.Attachment;
import edu.ku.brc.specify.datamodel.CollectingEventAttachment;
import edu.ku.brc.specify.datamodel.Collection;
import edu.ku.brc.specify.datamodel.CollectionObjectAttachment;
import edu.ku.brc.specify.datamodel.ConservDescriptionAttachment;
import edu.ku.brc.specify.datamodel.ConservEventAttachment;
import edu.ku.brc.specify.datamodel.DNASequencingRunAttachment;
import edu.ku.brc.specify.datamodel.Discipline;
import edu.ku.brc.specify.datamodel.Division;
import edu.ku.brc.specify.datamodel.FieldNotebookAttachment;
import edu.ku.brc.specify.datamodel.FieldNotebookPageAttachment;
import edu.ku.brc.specify.datamodel.FieldNotebookPageSetAttachment;
import edu.ku.brc.specify.datamodel.LoanAttachment;
import edu.ku.brc.specify.datamodel.LocalityAttachment;
import edu.ku.brc.specify.datamodel.PermitAttachment;
import edu.ku.brc.specify.datamodel.PreparationAttachment;
import edu.ku.brc.specify.datamodel.RepositoryAgreementAttachment;
import edu.ku.brc.specify.datamodel.TaxonAttachment;
import edu.ku.brc.ui.CustomDialog;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.ui.dnd.SimpleGlassPane;
import edu.ku.brc.util.AttachmentUtils;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Jan 14, 2010
 *
 */
public class FixDBAfterLogin
{
    private static final Logger log  = Logger.getLogger(FixDBAfterLogin.class);
    
    private final static String FIX_DEFDATES_PREF = "FIX_DEFDATES_PREF";
    
    public final static String DEGREES_SYMBOL = "\u00b0";
    public final static String SEPS           = DEGREES_SYMBOL + ":'\" ";
    
    /**
     * 
     */
    public FixDBAfterLogin()
    {
        super();
    }
    
    /**
     * 
     */
    public void checkMultipleLocalities()
    {
        int cnt = getCountAsInt("select count(localitydetailid) - count(distinct localityid) from localitydetail");
        if (cnt > 0)
		{
			cnt = getCountAsInt("select count(collectionobjectid) from collectionobject co " +
					"inner join collectingevent ce on ce.collectingeventid = co.collectingeventid " +
					"inner join  (select localityid from localitydetail group by localityid having count(localitydetailid) > 1) badlocs on badlocs.localityid = ce.localityid");
			String str = String.format("Multiple Locality Detail Records - Count: %d", cnt);
			edu.ku.brc.exceptions.ExceptionTracker.getInstance().sendMsg(FixDBAfterLogin.class, str, new Exception(str));
		}
         
         cnt = getCountAsInt("select count(geocoorddetailid) - count(distinct localityid) from geocoorddetail");
         if (cnt > 0)
         {
        	 cnt = getCountAsInt("select count(collectionobjectid) from collectionobject co " +
        	 		"inner join collectingevent ce on ce.collectingeventid = co.collectingeventid " +
        	 		"inner join (select localityid from geocoorddetail group by localityid having count(geocoorddetailid) > 1) badlocs on badlocs.localityid = ce.localityid");
 		     String str = String.format("Multiple GeoCoord Detail Records - Count: %d", cnt);
 		     edu.ku.brc.exceptions.ExceptionTracker.getInstance().sendMsg(FixDBAfterLogin.class, str, new Exception(str));
         }
    }
    
    /**
     * fixes bad version and timestamps for recordsets created by Uploader. 
     */
    public void fixUploaderRecordsets()
    {
    	update( "update recordset set TimestampCreated = now(), Version = 0 where Type = 1 and Version is null");
        AppPreferences.getGlobalPrefs().putBoolean("FixUploaderRecordsets", true);
    }
    
    /**
     * @param pStmt
     * @param locId
     * @param origUnit
     * @param srcUnit
     * @param lat1Text
     * @param long1Text
     * @param isNew
     * @return
     * @throws SQLException
     */
    private boolean fixLatLong(final PreparedStatement pStmt, 
                               final int locId, 
                               final int origUnit, 
                               final int srcUnit, 
                               final String lat1Text, 
                               final String long1Text,
                               final boolean isNew) throws SQLException
    {
        pStmt.setInt(3, locId);
        
        boolean doUpdate = false;
        if (isNew)
        {
            // These could be entered from Sp5 or Sp6
            int numColonsLat = lat1Text  != null ? StringUtils.countMatches(lat1Text, ":") : 0;
            int numColonsLon = long1Text != null ? StringUtils.countMatches(long1Text, ":") : 0;
            
            if (numColonsLat == 2 || numColonsLon == 2) // definitely Deg, Min Dec Secs
            {
                if (srcUnit != 1)
                {
                    //log.debug(String.format("1 - Lat[%s]  Lon[%s]  origUnit %d  srcUnit %d", lat1Text, long1Text, origUnit, srcUnit));
                    pStmt.setInt(1, origUnit);  // How Viewed
                    pStmt.setInt(2, 1);         // How Stored
                    return true;
                }
                return false;
            }
            
            String[] latTokens    = StringUtils.split(lat1Text, SEPS);
            String[] lonTokens    = StringUtils.split(long1Text, SEPS);
            int      latLen       = latTokens.length;
            int      lonLen       = lonTokens.length;
            
            if (latLen == 4 || lonLen == 4)
            {
                if (srcUnit != 1)  // Deg, Min Dec Secs
                {
                    //log.debug(String.format("Fix1 Lat[%s]  Lon[%s]  origUnit %d  srcUnit %d", lat1Text, long1Text, origUnit, srcUnit));
                    pStmt.setInt(1, origUnit);  // How Viewed
                    pStmt.setInt(2, 1);         // How Stored
                    return true;
                }
                
            } else if (latLen == 3 || lonLen == 3)
            {
                if (srcUnit != 2) // Degrees Decimal Minutes
                {
                    //log.debug(String.format("Fix2 Lat[%s]  Lon[%s]  origUnit %d  srcUnit %d", lat1Text, long1Text, origUnit, srcUnit));
                    pStmt.setInt(1, origUnit);  // How Viewed
                    pStmt.setInt(2, 2);         // How Stored
                    return true;
                }

            } else if (latLen == 2 || lonLen == 2)
            {
                if (srcUnit != 0) // Decimal Degrees 
                {
                    //log.debug(String.format("Fix0 Lat[%s]  Lon[%s]  origUnit %d  srcUnit %d", lat1Text, long1Text, origUnit, srcUnit));
                    pStmt.setInt(1, origUnit);  // How Viewed
                    pStmt.setInt(2, 1);         // How Stored
                    return true;
                }
            } else
            {
                log.debug(String.format("*** Couldn't parse Lat[%s]  Lon[%s]  origUnit %d  srcUnit %d", lat1Text, long1Text, origUnit, srcUnit));
            }
            
            
        } else
        {
            if (srcUnit == 0 && origUnit != 0)
            {
                pStmt.setInt(1, origUnit); // How Viewed
                pStmt.setInt(2, origUnit); // How Stored
                doUpdate = true;
            }
        }
        return doUpdate;
    }
    
    /**
     * 
     */
    public void fixLatLongUnit()
    {
        int     fixed = 0;
        //String  updateSQL = "UPDATE locality SET OriginalLatLongUnit=?, SrcLatLongUnit=?  WHERE LocalityID=?";
        String  postStr   = " WHERE OriginalLatLongUnit > 0 AND (Lat1Text IS NOT NULL OR Long1Text IS NOT NULL)";
        
        Connection        conn  = DBConnection.getInstance().getConnection();
        PreparedStatement pStmt = null;
        Statement         stmt  = null;
        try
        {
            int totalCnt = getCount(conn, "SELECT COUNT(*) FROM locality" + postStr);
            //log.debug("Total Count: "+totalCnt);
            if (totalCnt > 0)
            {
                pStmt = conn.prepareStatement(updateSQL);
                stmt  = conn.createStatement();
                
                int cnt = 0;
                ResultSet rs = stmt.executeQuery("SELECT LocalityID, OriginalLatLongUnit, SrcLatLongUnit, Lat1Text, Long1Text, (TimestampCreated > '2008-06-01 00:00:00') AS IsNew FROM locality" + postStr);
                while (rs.next())
                {
                    int     id        = rs.getInt(1);
                    int     origUnit  = rs.getInt(2);
                    int     srcUnit   = rs.getInt(3);
                    String  lat1Text  = rs.getString(4);
                    String  long1Text = rs.getString(5);
                    boolean isNew     = rs.getInt(6) == 1;
                    
                    if (fixLatLong(pStmt, id, origUnit, srcUnit, lat1Text, long1Text, isNew))
                    {
                        
                        if (pStmt.executeUpdate() != 1)
                        {
                            log.error("Error updating "+id);
                        } else
                        {
                            fixed++;
                        }
                    }
                    cnt++;
                    if (cnt % 100 == 0)
                    {
                        log.debug(String.format("Processing %d/%d", cnt, totalCnt));
                    }
                }
                rs.close();
                log.debug(String.format("Fixed %d/%d", fixed, totalCnt));
            }
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally
        {
            try
            {
                if (pStmt != null) pStmt.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {}
        }
    }
    
    /**
     * Creates empty CollectingEvents for CollectingEvent-less CollectionObjects in Collections 
     * with embedded CollectingEvents 
     */
    public static void fixNullEmbeddedCollectingEvents()
    {
    	try
    	{
    		String checkSQL = "select count(co.CollectionObjectID) from collectionobject co inner join "
    			+ "collection cln on cln.UserGroupScopeID = co.CollectionMemberID "
    			+ "where co.CollectingEventID is null and cln.IsEmbeddedCollectingEvent";
    		int cesToAdd = BasicSQLUtils.getCount(checkSQL);
    		if (cesToAdd > 0)
    		{
    			String ceInsertSql = "insert into collectingevent (TimestampCreated, TimestampModified, Version, " +
    				"Visibility, CreatedByAgentID, VisibilitySetByID, ModifiedByAgentID, DisciplineID)" +
    				" select co.TimestampCreated, co.TimestampModified, co.CollectionObjectID, co.Visibility, " +
    				"co.CreatedByAgentID, co.VisibilitySetByID, co.ModifiedByAgentID, " +
    				"(select DisciplineID from collection cln2 where cln2.UserGroupScopeID = co.CollectionMemberID) " +
    				"from collectionobject co inner join collection cln on cln.UserGroupScopeID = " +
    				"co.CollectionMemberID where co.CollectingEventID is null and cln.IsEmbeddedCollectingEvent";
    			int insertedCnt = BasicSQLUtils.update(ceInsertSql);
    			if (insertedCnt != cesToAdd)
    			{
    				throw new Exception("fixNullEmbeddedCollectingEvents: error inserting colllectingevent records");
    			}
    			String ceIDUpdateSql = "update collectionobject co inner join collectingevent ce on ce.Version = "
    				+ "co.CollectionObjectID inner join collection cln on cln.UserGroupScopeID = co.CollectionMemberID "
    				+ "set co.CollectingEventID = ce.CollectingEventID, ce.Version = 0 "
    				+ "where co.collectingeventid is null and cln.IsEmbeddedCollectingEvent";
    			int updatedCnt = BasicSQLUtils.update(ceIDUpdateSql);
    			if (updatedCnt != 2*insertedCnt)
    			{
    				throw new Exception("fixNullEmbeddedCollectingEvents: error updating colllectingevent IDs");
    			}
    			log.info("added " + insertedCnt + " collectingevent records");
    	        AppPreferences.getGlobalPrefs().putBoolean("FixNullEmbeddedCollectingEvents", true);
    	}
		} catch (Exception ex)
		{
            log.error(ex);
			ex.printStackTrace();
		}
    }
    
    /**
     * 
     */
    public static void fixUserPermissions(final boolean doSilently)
    {
        final String FIXED_USER_PERMS = "FIXED_USER_PERMS";
        boolean isAlreadyFixed = AppPreferences.getRemote().getBoolean(FIXED_USER_PERMS, false);
        if (isAlreadyFixed)
        {
            return;
        }
        
        String whereStr  = " WHERE p.GroupSubClass = 'edu.ku.brc.af.auth.specify.principal.UserPrincipal' ";
        String whereStr2 = "AND p.userGroupScopeID IS NULL";
        
        String postSQL = " FROM specifyuser su " +
                         "INNER JOIN specifyuser_spprincipal ss ON su.SpecifyUserID = ss.SpecifyUserID " +
                         "INNER JOIN spprincipal p ON ss.SpPrincipalID = p.SpPrincipalID " +
                         "LEFT JOIN spprincipal_sppermission pp ON p.SpPrincipalID = pp.SpPrincipalID " +
                         "LEFT OUTER JOIN sppermission pm ON pp.SpPermissionID = pm.SpPermissionID " +
                         whereStr;
        
        String sql = "SELECT COUNT(*)" + postSQL + whereStr2;
        log.debug(sql);
        if (BasicSQLUtils.getCountAsInt(sql) < 1)
        {
            sql = "SELECT COUNT(*)" + postSQL;
            log.debug(sql);
            if (BasicSQLUtils.getCountAsInt(sql) > 0)
            {
                return;
            }
        }
        
        final String updatePermSQL = "DELETE FROM %s WHERE SpPermissionID = %d";
        final String updatePrinSQL = "DELETE FROM %s WHERE SpPrincipalID = %d";

        sql = "SELECT p.SpPrincipalID, pp.SpPermissionID" + postSQL;
        log.debug(sql);
        
        HashSet<Integer> prinIds = new HashSet<Integer>();
        for (Object[] row :  query(sql))
        {
            Integer prinId = (Integer)row[0];
            if (prinId != null)
            {
                prinIds.add(prinId);
            }
            
            Integer permId = (Integer)row[1];
            if (permId != null)
            {
                update(String.format(updatePermSQL, "spprincipal_sppermission", permId));
                update(String.format(updatePermSQL, "sppermission", permId));
                log.debug("Removing PermId: "+permId);
            }
        }
        
        StringBuilder sb1 = new StringBuilder();
        for (Integer prinId : prinIds)
        {
            update(String.format(updatePrinSQL, "specifyuser_spprincipal", prinId));
            update(String.format(updatePrinSQL, "spprincipal", prinId));
            log.debug("Removing PrinId: "+prinId);
            if (sb1.length() > 0) sb1.append(",");
            sb1.append(prinId.toString());
        }
        log.debug("("+sb1.toString()+")");
        
        // Create all the necessary UperPrincipal records
        // Start by figuring out what group there are and then create one UserPrincipal record
        // for each one
        
        TreeSet<String> nameSet = new TreeSet<String>();

        sql = "SELECT su.Name, su.SpecifyUserID, p.userGroupScopeID, p.SpPrincipalID FROM specifyuser su " +
        	  "INNER JOIN specifyuser_spprincipal sp ON su.SpecifyUserID = sp.SpecifyUserID " +
              "INNER JOIN spprincipal p ON sp.SpPrincipalID = p.SpPrincipalID " +
              "WHERE p.GroupSubClass = 'edu.ku.brc.af.auth.specify.principal.GroupPrincipal'";
        
        String fields     = "TimestampCreated, TimestampModified, Version, GroupSubClass, groupType, Name, Priority, Remarks, userGroupScopeID, CreatedByAgentID, ModifiedByAgentID";
        String insertSQL  = "INSERT INTO spprincipal ("+fields+") VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        String insertSQL2 = "INSERT INTO specifyuser_spprincipal (SpecifyUserID, SpPrincipalID) VALUES(?,?)";
        
        String searchSql = "SELECT "+fields+" FROM spprincipal WHERE SpPrincipalID = ?";

        sb1 = new StringBuilder();
        
        PreparedStatement selStmt = null;
        PreparedStatement pStmt   = null;
        PreparedStatement pStmt2  = null;
        try
        {
            Connection conn = DBConnection.getInstance().getConnection();
            
            pStmt   = conn.prepareStatement(insertSQL);
            pStmt2  = conn.prepareStatement(insertSQL2);
            selStmt = conn.prepareStatement(searchSql);
            
            String adtSQL = "SELECT DISTINCT ca.AgentID FROM specifyuser AS su INNER Join agent AS ca ON su.CreatedByAgentID = ca.AgentID";
            Integer createdById = BasicSQLUtils.getCount(conn, adtSQL);
            if (createdById == null)
            {
                createdById = BasicSQLUtils.getCount(conn, "SELECT AgentID FROM agent ORDER BY AgentID ASC LIMIT 0,1");
                if (createdById == null)
                {
                    UIRegistry.showError("The permissions could not be fixed because there were no agents.");
                    AppPreferences.shutdownAllPrefs();
                    DBConnection.shutdownFinalConnection(true, true);
                    return;
                }
            }
            
            for (Object[] row : query(sql))
            {
                String  usrName = (String)row[0];
                Integer userId  = (Integer)row[1];
                Integer collId  = (Integer)row[2];
                Integer prinId  = (Integer)row[3];
             
                nameSet.add(usrName);
                
                log.debug("usrName: " + usrName + "  prinId: "+prinId);
                if (sb1.length() > 0) sb1.append(",");
                sb1.append(prinId.toString());
                
                selStmt.setInt(1, prinId);
                ResultSet rs = selStmt.executeQuery();
                if (rs.next())
                {
                    log.debug(String.format("%s - adding UserPrincipal for Collection  %d / %d", usrName, rs.getInt(9), collId));
                    Integer createdByAgentID  = (Integer)rs.getObject(10);
                    Integer modifiedByAgentID = (Integer)rs.getObject(11);
                    
                    pStmt.setTimestamp(1, rs.getTimestamp(1));
                    pStmt.setTimestamp(2, rs.getTimestamp(2));
                    pStmt.setInt(3,    1);                                                    // Version
                    pStmt.setString(4, "edu.ku.brc.af.auth.specify.principal.UserPrincipal"); // GroupSubClass
                    pStmt.setString(5, null);                                                 // groupType
                    pStmt.setString(6, rs.getString(6));                                      // Name
                    pStmt.setInt(7,    80);                                                   // Priority
                    pStmt.setString(8, rs.getString(8));                                      // Remarks
                    pStmt.setInt(9,    rs.getInt(9));                                         // userGroupScopeID
                    pStmt.setInt(10,   createdByAgentID != null ? createdByAgentID : createdById);
                    pStmt.setInt(11,   modifiedByAgentID != null ? modifiedByAgentID : createdById);
                    
                    // Create UserPrincipal
                    pStmt.executeUpdate();
                    
                    int newPrinId = BasicSQLUtils.getInsertedId(pStmt);
                    
                    // Join the new Principal to the SpecifyUser record
                    pStmt2.setInt(1, userId);
                    pStmt2.setInt(2, newPrinId);
                    pStmt2.executeUpdate();
                    
                } else
                {
                    // error
                }
                rs.close();
            }
            
            log.debug("("+sb1.toString()+")");
            
            AppPreferences.getRemote().putBoolean(FIXED_USER_PERMS, true);
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
        } finally
        {
            try
            {
                if (pStmt != null) pStmt.close();
                if (pStmt2 != null) pStmt2.close();
                if (selStmt != null) selStmt.close();
            } catch (Exception ex){}
        }
        
        final StringBuilder sb = new StringBuilder();
        for (String nm : nameSet)
        {
            if (sb.length() > 0) sb.append('\n');
            sb.append(nm);
        }
        
        if (!doSilently)
        {
            JTextArea ta = UIHelper.createTextArea(15, 30);
            ta.setText(sb.toString());
            ta.setEditable(false);
            
            JEditorPane htmlPane   = new JEditorPane("text/html",  UIRegistry.getResourceString("FDBAL_PERMFIXEDDESC")); //$NON-NLS-1$
            htmlPane.setEditable(false);
            htmlPane.setOpaque(false);
            
            CellConstraints cc = new CellConstraints();
            PanelBuilder    pb = new PanelBuilder(new FormLayout("f:p:g", "p:g,8px,f:p:g"));
            pb.add(htmlPane, cc.xy(1, 1));
            pb.add(UIHelper.createScrollPane(ta), cc.xy(1, 3));
            pb.setDefaultDialogBorder();
            
            CustomDialog dlg = new CustomDialog((Frame)UIRegistry.getMostRecentWindow(), UIRegistry.getResourceString("FDBAL_PERMFIXED"), true, CustomDialog.OK_BTN, pb.getPanel());
            dlg.setOkLabel(UIRegistry.getResourceString("CLOSE"));
            UIHelper.centerAndShow(dlg);
        }
    }
    
    /**
     * 
     */
    public static void fixDefaultDates()
    {
        boolean doFix = !AppPreferences.getGlobalPrefs().getBoolean(FIX_DEFDATES_PREF, false);
        //log.debug("fixDefaultDates -  Going To Fix["+doFix+"]");
        if (doFix)
        {
            HashMap<DBTableInfo, List<FormCellFieldIFace>>  tblToFldHash = new HashMap<DBTableInfo, List<FormCellFieldIFace>>();
            HashSet<String> nameHash = new HashSet<String>();
            
            for (ViewIFace view : ((SpecifyAppContextMgr)AppContextMgr.getInstance()).getEntirelyAllViews())
            {
                String tableClassName = view.getClassName();
                DBTableInfo ti = DBTableIdMgr.getInstance().getByClassName(tableClassName);
                if (ti != null)
                {
                	if (nameHash.contains(tableClassName))
                	{
                		continue;
                	}
                	nameHash.add(tableClassName);
                	
                	//log.debug(tableClassName);
                	
                    for (AltViewIFace avi : view.getAltViews())
                    {
                    	if (avi.getMode() == AltViewIFace.CreationMode.EDIT)
                    	{
	                        ViewDefIFace vd = (ViewDefIFace)avi.getViewDef();
	                        if (vd instanceof FormViewDef)
	                        {
	                            FormViewDefIFace fvd = (FormViewDefIFace)vd;
	                            for (FormRowIFace fri : fvd.getRows())
	                            {
	                                for (FormCellIFace fci : fri.getCells())
	                                {
	                                    if (fci instanceof FormCellFieldIFace)
	                                    {
		                                	//log.debug(ti.getName()+" - "+fci.getIdent()+"  "+fci.getName());
		                                	
	                                        FormCellFieldIFace fcf      = (FormCellFieldIFace)fci;
	                                        String             defValue = fcf.getDefaultValue();
	                                        if (StringUtils.isNotEmpty(defValue) && !defValue.equals("Today"))
	                                        {
	                                            List<FormCellFieldIFace> fieldList = tblToFldHash.get(ti);
	                                            if (fieldList == null)
	                                            {
	                                                fieldList = new ArrayList<FormCellFieldIFace>();
	                                                tblToFldHash.put(ti, fieldList);
	                                            }
	                                            fieldList.add(fcf);
	                                        }
	                                    }
	                                }
	                            }
                            }
                        }
                    }
                //} else
                //{
                    //log.debug("Skipping table Class Name["+tableClassName+"]");
                }
            }
            
            log.debug("Number of Tables Found["+tblToFldHash.size()+"]");
            processTableDefaultDates(tblToFldHash, false);
            
            AppPreferences.getGlobalPrefs().putBoolean(FIX_DEFDATES_PREF, true);
        }
    }
    
    /**
     * @param tblToFldHash
     * @param doingCount
     * @return
     */
    private static int processTableDefaultDates(final HashMap<DBTableInfo, List<FormCellFieldIFace>>  tblToFldHash, final boolean doingCount)
    {
        int totalCount = 0;
        for (DBTableInfo ti : tblToFldHash.keySet())
        {
        	//log.debug("processTableDefaultDates - Fixing table "+ti.getName());
            for (FormCellFieldIFace fci : tblToFldHash.get(ti))
            {
                String[] names = fci.getFieldNames();
                if (names != null && names.length > 0 && names.length < 3)
                {
                	Class<?> dataClass = null;
                	String   fldName   = null;
                	if (fci.getUiType() == FormCellFieldIFace.FieldType.plugin) // assumes plugin
                	{
                		String pluginName = fci.getProperty("name"); 
                		if (StringUtils.isNotEmpty(pluginName) && pluginName.equals("PartialDateUI"))
                		{
                			String dataField = fci.getProperty("df"); 
                			if (StringUtils.isNotEmpty(dataField))
                    		{
                				fldName   = dataField;
                				dataClass = java.util.Calendar.class;
                    		}
                		}
                	} else
                	{
                		fldName = names[0];
                	}
                	
                    DBFieldInfo fi = ti.getFieldByName(fldName);
                    if (fi != null)
                    {
                    	if (dataClass == null)
                        {
                        	dataClass = fi.getDataClass();
                        }
                    	
                    	if (dataClass == java.util.Calendar.class)
                    	{
	                       	//log.debug("processTableDefaultDates - Fixing field "+fi.getColumn());
	                        String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s IS NULL AND TimestampCreated IS NOT NULL AND TimestampCreated > TIMESTAMP('2008-06-01 00:00:00')", ti.getName(), fi.getColumn());
	                        int    cnt = BasicSQLUtils.getCountAsInt(sql);
	                        if (cnt > 0)
	                        {
	                            if (doingCount)
	                            {
	                                totalCount += cnt;
	                            } else
	                            {
	                            	totalCount += fixTableDefaultDates(ti, fi);
	                            }
	                        }
                    	}
                    }
                }
            }
        }
        
        log.debug("processTableDefaultDates - Fixed "+totalCount+" Records.");
        
        return 0;
    }
    
    private static String updateSQL     = "UPDATE %s SET %s = DATE(TimestampCreated) WHERE %s IS NULL AND TimestampCreated IS NOT NULL AND TimestampCreated > TIMESTAMP('2008-06-01 00:00:00')";
    private static String updatePrecSQL = "UPDATE %s SET %s = 1 WHERE %s IS NULL AND %s IS NULL AND TimestampCreated IS NOT NULL AND TimestampCreated > TIMESTAMP('2008-06-01 00:00:00')";

    /**
     * @param ti
     * @param fi
     * @return
     */
    private static int fixTableDefaultDates(final DBTableInfo ti, final DBFieldInfo fi)
    {
        String colName = fi.getColumn();
        if (colName.length() > 4 && colName.endsWith("Date"))
        {
            String precName = colName + "Precision";
            if (ti.getFieldByColumnName(precName) != null)
            {
                String sql = String.format(updatePrecSQL, ti.getName(), precName, precName, fi.getColumn());
                BasicSQLUtils.update(sql);
            }
        }
        String sql = String.format(updateSQL, ti.getName(), fi.getColumn(), fi.getColumn());
        int rv = BasicSQLUtils.update(sql);

        return rv;
    }

    
    /**
     * @param agentId
     * @param colMemId
     * @param inclColMemId
     * @param tableId
     * @return
     */
    private Vector<Object[]> getImageData(final int agentId, 
                                                 final int tableId,
                                                 final HashMap<Integer, String> tblTypeHash) throws Exception
    {
        DBTableInfo ti = DBTableIdMgr.getInstance().getInfoById(tableId);
        
        String sql = String.format("SELECT a.AttachmentID, a.AttachmentLocation, a.OrigFilename, ag.AgentID " +
                 	 "FROM attachment a INNER JOIN %s x ON a.AttachmentID = x.AttachmentID " +
                     "INNER JOIN agent ag ON a.CreatedByAgentID = ag.AgentID " +
                     "WHERE a.AttachmentLocation LIKE 'xxx.att.%c' AND ag.AgentID = %d", ti.getName(), '%', agentId);
        
        String title = ti.getTitle();
        if (ti.getRelationshipByName("division") != null)
        {
            Division div = AppContextMgr.getInstance().getClassObject(Division.class);
            sql += " AND x.DivisionID = " + div.getId();
            title += " (Division Level)";//div.getName());
            
        } else if (ti.getFieldByColumnName("CollectionMemberID") != null)
        {
            Collection collection = AppContextMgr.getInstance().getClassObject(Collection.class);
            sql += " AND x.CollectionMemberID = " + collection.getId();
            title += " (Collection Level)";//tblTypeHash.put(tableId, collection.getCollectionName());
            
        } else if (ti.getRelationshipByName("discipline") != null)
        {
            Discipline dsp = AppContextMgr.getInstance().getClassObject(Discipline.class);
            sql += " AND x.DisciplineID = " + dsp.getId();
            title += " (Discipline Level)";//tblTypeHash.put(tableId, "Discipline: "+dsp.getName());
        } else
        {
            System.err.println("Error: "+title);
            title += " (Global Level)";
        }
        tblTypeHash.put(tableId, title);
        //System.out.println(sql);
        return BasicSQLUtils.query(sql);
    }
    
    /**
     * @param resultsHashMap
     * @param tableHash
     * @param totalFiles
     */
    private void reattachFiles(final HashMap<Integer, Vector<Object[]>> resultsHashMap,
                               final HashMap<Integer, AttchTableModel> tableHash,
                               final int totalFiles)
    {
        final String CNT  = "CNT";
        final SwingWorker<Integer, Integer> worker = new SwingWorker<Integer, Integer>()
        {
            int filesCnt = 0;
            
            @Override
            protected Integer doInBackground() throws Exception
            {
                DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
                if (session != null)
                {
                    try
                    {
                        for (int tblId : resultsHashMap.keySet())
                        {
                            AttchTableModel model = tableHash.get(tblId);
                            for (int r=0;r<model.getRowCount();r++)
                            {
                                if (model.isRecoverable(r))
                                {
                                    Thread.currentThread().sleep(100);
                                    
                                    session.beginTransaction();
                                    Integer    attachID   = model.getAttachmentId(r);
                                    Attachment attachment = session.get(Attachment.class, attachID);
                                    AttachmentUtils.getAttachmentManager().setStorageLocationIntoAttachment(attachment, false);
                                    attachment.storeFile(true); // false means do not display an error dialog
                                    session.saveOrUpdate(attachment);
                                    session.commit();
                                    filesCnt++;
                                    firePropertyChange(CNT, 0, (int)((double)filesCnt / (double)totalFiles * 100.0));
                                }
                            }
                        }
                    } catch (Exception ex)
                    {
                        session.rollback();
                        
                    } finally
                    {
                        session.close();
                    }
                }
                return null;
            }

            @Override
            protected void done()
            {
                UIRegistry.clearSimpleGlassPaneMsg();
                UIRegistry.displayInfoMsgDlg(String.format("Files recovered: %d / %d", filesCnt, totalFiles));
                super.done();
            }
        };
        
        final SimpleGlassPane glassPane = UIRegistry.writeSimpleGlassPaneMsg(String.format("Recovering %d files.", totalFiles), 24);
        glassPane.setProgress(0);
        
        worker.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public  void propertyChange(final PropertyChangeEvent evt) {
                        if (CNT.equals(evt.getPropertyName())) 
                        {
                            glassPane.setProgress((Integer)evt.getNewValue());
                        }
                    }
                });
        
        worker.execute();
    }
    
    /**
     * 
     */
    public void checkForBadAttachments()
    {
        try
        {
            String sql = "SELECT COUNT(*) FROM attachment WHERE AttachmentLocation LIKE 'xxx.att%'";
            int count = BasicSQLUtils.getCountAsInt(sql);
            if (count == 0)
            {
                return;
            }
            
            int totalFiles = 0;
            HashMap<Integer, Vector<Object[]>> resultsHashMap = new HashMap<Integer, Vector<Object[]>>();
            HashMap<Integer, String>           tblTypeHash    = new HashMap<Integer, String>();
            int[] tableIds = 
            { 
                    AccessionAttachment.getClassTableId(), AgentAttachment.getClassTableId(), 
                    CollectingEventAttachment.getClassTableId(), CollectionObjectAttachment.getClassTableId(), 
                    ConservDescriptionAttachment.getClassTableId(), ConservEventAttachment.getClassTableId(), 
                    DNASequencingRunAttachment.getClassTableId(), FieldNotebookAttachment.getClassTableId(), 
                    FieldNotebookPageAttachment.getClassTableId(), FieldNotebookPageSetAttachment.getClassTableId(), 
                    LoanAttachment.getClassTableId(), LocalityAttachment.getClassTableId(), 
                    PermitAttachment.getClassTableId(), PreparationAttachment.getClassTableId(), 
                    RepositoryAgreementAttachment.getClassTableId(), TaxonAttachment.getClassTableId()
            };
    
            Agent userAgent  = AppContextMgr.getInstance().getClassObject(Agent.class);
            
            for (int tableId : tableIds)
            {
                Vector<Object[]> results = getImageData(userAgent.getId(), tableId, tblTypeHash);
                if (results != null && results.size() > 0)
                {
                    resultsHashMap.put(tableId, results);
                    //System.out.println(tableId+"  ->  "+results.size());
                }
            }
            
            if (resultsHashMap.size() == 0) // Shouldn't happen
            {
                return;
            }
            
            CellConstraints cc = new CellConstraints();
            HashMap<Integer, String>          agentHash = new HashMap<Integer, String>();
            HashMap<Integer, AttchTableModel> tableHash = new HashMap<Integer, AttchTableModel>();
            
            int          y      = 1;
            String       rowDef = UIHelper.createDuplicateJGoodiesDef("p", "4px", resultsHashMap.size());
            PanelBuilder pb     = new PanelBuilder(new FormLayout("f:p:g", rowDef));
            for (int tblId : resultsHashMap.keySet())
            {
                Vector<Object[]> dataRows = new Vector<Object[]>();
                Vector<Object[]> results = resultsHashMap.get(tblId);
                for (Object[] row : results)
                {
                    Integer agentId = (Integer)row[3];
                    String userName = agentHash.get(agentId); 
                    if (userName == null)
                    {
                        userName = BasicSQLUtils.querySingleObj("SELECT su.Name FROM agent a INNER JOIN specifyuser su ON a.SpecifyUserID = su.SpecifyUserID WHERE a.AgentID = "+row[3]);
                        agentHash.put(agentId, userName);
                    }
                    boolean doesExist = (new File((String)row[2])).exists();
                    String fullPath = (String)row[2];
                    Object[] rowObjs = new Object[5];
                    rowObjs[0] = FilenameUtils.getName(fullPath);
                    rowObjs[1] = fullPath;
                    rowObjs[2] = userName;
                    rowObjs[3] = doesExist;
                    rowObjs[4] = row[0];
                    
                    dataRows.add(rowObjs);
                    
                    if (doesExist)
                    {
                        totalFiles++;
                    }
                }
                PanelBuilder    pb2   = new PanelBuilder(new FormLayout("f:p:g", "p,2px,f:p:g"));
                AttchTableModel model = new AttchTableModel(dataRows);
                JTable          table = new JTable(model);
                if (resultsHashMap.size() > 1)
                {
                    UIHelper.calcColumnWidths(table, dataRows.size() < 15 ? dataRows.size()+1 : 15);
                } else
                {
                    UIHelper.calcColumnWidths(table);
                }
                pb2.add(UIHelper.createLabel(tblTypeHash.get(tblId)), cc.xy(1, 1));
                pb2.add(UIHelper.createScrollPane(table), cc.xy(1, 3));
                pb.add(pb2.getPanel(), cc.xy(1, y));
                tableHash.put(tblId, model);
                y += 2;
            }
            
            CustomDialog dlg = new CustomDialog((Dialog)null, "Attachment Information", true, CustomDialog.OKCANCEL, UIHelper.createScrollPane(pb.getPanel()));
            dlg.setCancelLabel("Skip");
            dlg.setOkLabel("Recover Files");
            dlg.createUI();
            dlg.pack();
            Dimension dim = dlg.getSize();
            dim.height = Math.max(600, dim.height);
            dlg.setSize(dim);
            dlg.setVisible(true);
            
            if (!dlg.isCancelled())
            {
                reattachFiles(resultsHashMap, tableHash, totalFiles);
            }
            
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
    
    
    class AttchTableModel extends DefaultTableModel
    {
        protected String[] headers = {"File Name", "Full Path", "Agent", "Is Recoverable"};
        protected Vector<Object[]> dataRows;
        
        /**
         * @param fileName
         * @param agentName
         */
        public AttchTableModel(final Vector<Object[]> dataRows)
        {
            super();
            this.dataRows = dataRows;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.DefaultTableModel#getColumnCount()
         */
        @Override
        public int getColumnCount()
        {
            return headers != null ? headers.length : 0;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.DefaultTableModel#getColumnName(int)
         */
        @Override
        public String getColumnName(int column)
        {
            return headers[column];
        }

        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
         */
        @Override
        public Class<?> getColumnClass(int col)
        {
            return col == 3 ? Boolean.class : String.class;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.DefaultTableModel#getRowCount()
         */
        @Override
        public int getRowCount()
        {
            return dataRows != null ? dataRows.size() : 0;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.DefaultTableModel#getValueAt(int, int)
         */
        @Override
        public Object getValueAt(int row, int column)
        {
            Object[] r = dataRows.get(row);
            return r[column];
        }

        /* (non-Javadoc)
         * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
         */
        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
        
        public int getAttachmentId(final int row)
        {
            Object[] r = dataRows.get(row);
            return (Integer)r[r.length-1];
        }
        
        public boolean isRecoverable(final int row)
        {
            Object[] r = dataRows.get(row);
            return (Boolean)r[3];
        }
    }
}
