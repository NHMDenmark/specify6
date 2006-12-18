/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ku.brc.specify.tasks;

import static edu.ku.brc.ui.UICacheManager.getResourceString;

import java.awt.Component;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.AppResourceIFace;
import edu.ku.brc.af.core.ContextMgr;
import edu.ku.brc.af.core.MenuItemDesc;
import edu.ku.brc.af.core.NavBox;
import edu.ku.brc.af.core.NavBoxAction;
import edu.ku.brc.af.core.NavBoxButton;
import edu.ku.brc.af.core.NavBoxIFace;
import edu.ku.brc.af.core.NavBoxItemIFace;
import edu.ku.brc.af.core.NavBoxMgr;
import edu.ku.brc.af.core.SubPaneIFace;
import edu.ku.brc.af.core.SubPaneMgr;
import edu.ku.brc.af.core.TaskCommandDef;
import edu.ku.brc.af.core.TaskMgr;
import edu.ku.brc.af.core.Taskable;
import edu.ku.brc.af.core.ToolBarItemDesc;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.tasks.BaseTask;
import edu.ku.brc.af.tasks.subpane.FormPane;
import edu.ku.brc.af.tasks.subpane.SimpleDescPane;
import edu.ku.brc.dbsupport.DBTableIdMgr;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.dbsupport.TableModel2Excel;
import edu.ku.brc.helpers.EMailHelper;
import edu.ku.brc.helpers.Encryption;
import edu.ku.brc.helpers.SwingWorker;
import edu.ku.brc.specify.config.SpecifyAppContextMgr;
import edu.ku.brc.specify.datamodel.Agent;
import edu.ku.brc.specify.datamodel.CollectionObjDef;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.InfoRequest;
import edu.ku.brc.specify.datamodel.Loan;
import edu.ku.brc.specify.datamodel.LoanPhysicalObject;
import edu.ku.brc.specify.datamodel.LoanReturnPhysicalObject;
import edu.ku.brc.specify.datamodel.Preparation;
import edu.ku.brc.specify.datamodel.RecordSet;
import edu.ku.brc.specify.datamodel.Shipment;
import edu.ku.brc.specify.ui.LoanReturnDlg;
import edu.ku.brc.specify.ui.LoanSelectPrepsDlg;
import edu.ku.brc.specify.ui.LoanReturnDlg.LoanReturnInfo;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.JStatusBar;
import edu.ku.brc.ui.ToolBarDropDownBtn;
import edu.ku.brc.ui.Trash;
import edu.ku.brc.ui.UICacheManager;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.db.ViewBasedDisplayDialog;
import edu.ku.brc.ui.forms.FormHelper;
import edu.ku.brc.ui.forms.FormViewObj;
import edu.ku.brc.ui.forms.MultiView;
import edu.ku.brc.ui.forms.TableViewObj;
import edu.ku.brc.ui.forms.Viewable;
import edu.ku.brc.ui.forms.persist.View;

/**
 * This task manages Loans, Gifts, Exchanges and provide actions and forms to do the interactions
 *
 * @code_status Beta
 *
 * @author rods
 *
 */
public class InteractionsTask extends BaseTask
{
    private static final Logger log = Logger.getLogger(InteractionsTask.class);

    public static final String     INTERACTIONS        = "Interactions";
    public static final DataFlavor INTERACTIONS_FLAVOR = new DataFlavor(DataEntryTask.class, INTERACTIONS);
    public static final DataFlavor INFOREQUEST_FLAVOR  = new DataFlavor(InfoRequest.class, INTERACTIONS);

    protected static final String InfoRequestName = "InfoRequest";
    protected static final String NEW_LOAN        = "New_Loan";
    protected static final String PRINT_LOAN      = "PrintLoan";
    protected static final String INFO_REQ_MESSAGE = "Specify Info Request";
    protected static final String CREATE_MAILMSG   = "CreateMailMsg";
    
    protected final int loanTableId;
    protected final int infoRequestTableId;
    protected final int colObjTableId;

    // Data Members
    protected NavBox              infoRequestNavBox;
    protected Vector<NavBoxIFace> extendedNavBoxes = new Vector<NavBoxIFace>();

   /**
     * Default Constructor
     *
     */
    public InteractionsTask()
    {
        super(INTERACTIONS, getResourceString("Interactions"));
        
        CommandDispatcher.register(INTERACTIONS, this);
        CommandDispatcher.register(RecordSetTask.RECORD_SET, this);
        CommandDispatcher.register(APP_CMD_TYPE, this);
        CommandDispatcher.register(DB_CMD_TYPE, this);
        CommandDispatcher.register(DataEntryTask.DATA_ENTRY, this);
        
        loanTableId        = DBTableIdMgr.lookupIdByClassName(Loan.class.getName());
        infoRequestTableId = DBTableIdMgr.lookupIdByClassName(InfoRequest.class.getName());
        colObjTableId      = DBTableIdMgr.lookupIdByClassName(CollectionObject.class.getName());
        
        this.icon = IconManager.getIcon(INTERACTIONS, IconManager.IconSize.Std16);

    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.core.Taskable#initialize()
     */
    public void initialize()
    {
        if (!isInitialized)
        {
            super.initialize(); // sets isInitialized to false
            
            extendedNavBoxes.clear();
            //labelsList.clear();

            // Temporary
            NavBox navBox = new NavBox(getResourceString("Actions"));
            NavBoxButton roc = (NavBoxButton)addToNavBoxAndRegisterAsDroppable(navBox, NavBox.createBtn(getResourceString(NEW_LOAN),  "Loan", IconManager.IconSize.Std16, new NavBoxAction(INTERACTIONS, NEW_LOAN)), null);
            roc.addDropDataFlavor(InfoRequestTask.INFOREQUEST_FLAVOR);
            
            navBox.add(NavBox.createBtn(getResourceString("New_Gifts"), "Loan", IconManager.IconSize.Std16));
            navBox.add(NavBox.createBtn(getResourceString("New_Exchange"), "Loan", IconManager.IconSize.Std16));
            addToNavBoxAndRegisterAsDroppable(navBox, NavBox.createBtn(getResourceString(InfoRequestName),  InfoRequestName, IconManager.IconSize.Std16, new NavBoxAction(INTERACTIONS, InfoRequestName, this)), null);
            navBoxes.addElement(navBox);
    
            // These need to be loaded as Resources
            navBox = new NavBox(getResourceString(ReportsTask.REPORTS));
            navBox.add(NavBox.createBtn(getResourceString("All_Overdue_Loans_Report"), ReportsTask.REPORTS, IconManager.IconSize.Std16));
            //navBox.add(NavBox.createBtn(getResourceString("All_Open_Loans_Report"), ReportsTask.REPORTS, IconManager.IconSize.Std16));
            //navBox.add(NavBox.createBtn(getResourceString("All_Loans_Report"), ReportsTask.REPORTS, IconManager.IconSize.Std16));
            //addToNavBoxAndRegisterAsDroppable(navBox, NavBox.createBtn(getResourceString(PRINT_LOAN),  ReportsTask.REPORTS, IconManager.IconSize.Std16, new NavBoxAction(INTERACTIONS, PRINT_LOAN, this)), null);
            navBoxes.addElement(navBox);
            
            // Then add
            if (commands != null)
            {
                for (AppResourceIFace ap : AppContextMgr.getInstance().getResourceByMimeType("jrxml/report"))
                {
                    Map<String, String> params = ap.getMetaDataMap();
                    params.put("title", ap.getDescription());
                    params.put("file", ap.getName());
                    //log.info("["+ap.getDescription()+"]["+ap.getName()+"]");
                    
                    commands.add(new TaskCommandDef(ap.getDescription(), name, params));
                }
                
                for (TaskCommandDef tcd : commands)
                {
                    // XXX won't be needed when we start validating the XML
                    String tableIdStr = tcd.getParams().get("tableid");
                    if (tableIdStr == null)
                    {
                        log.error("Interaction Command is missing the table id");
                    } else
                    {
                        addToNavBoxAndRegisterAsDroppable(navBox, NavBox.createBtn(tcd.getName(), "Loan", IconManager.IconSize.Std16, new NavBoxAction(tcd, this)), tcd.getParams());
                    }
                }
            }
            navBoxes.addElement(navBox);
            
            // Load InfoRequests into NavBox
            infoRequestNavBox  = new NavBox(getResourceString("InfoRequest"));
            DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
            
            List infoRequests = session.getDataList(InfoRequest.class);
            for (Iterator iter=infoRequests.iterator();iter.hasNext();)
            {
                InfoRequest infoRequest = (InfoRequest)iter.next();
                
                NavBoxItemIFace nbi = addNavBoxItem(infoRequestNavBox, infoRequest.getIdentityTitle(), INTERACTIONS, INTERACTIONS, "Delete", infoRequest);
                setUpDraggable(nbi, new DataFlavor[]{Trash.TRASH_FLAVOR, INFOREQUEST_FLAVOR}, new NavBoxAction("", ""));
            }      
            navBoxes.addElement(infoRequestNavBox);
            session.close();
        }
    }

    /**
     * Helper method for registering a NavBoxItem as a GhostMouseDropAdapter
     * @param navBox the parent box for the nbi to be added to
     * @param navBoxItemDropZone the nbi in question
     * @return returns the new NavBoxItem
     */
    protected NavBoxItemIFace addToNavBoxAndRegisterAsDroppable(final NavBox              navBox,
                                                                final NavBoxItemIFace     nbi,
                                                                final Map<String, String> params)
    {
        NavBoxButton roc = (NavBoxButton)nbi;
        roc.setData(params);

        // When Being Dragged
        roc.addDragDataFlavor(Trash.TRASH_FLAVOR);
        roc.addDragDataFlavor(INTERACTIONS_FLAVOR);

        // When something is dropped on it
        roc.addDropDataFlavor(RecordSetTask.RECORDSET_FLAVOR);

        navBox.add(nbi);
        //labelsList.add(nbi);
        return nbi;
    }
    
    /*
     *  (non-Javadoc)
     * @see edu.ku.brc.specify.core.Taskable#getNavBoxes()
     */
    public java.util.List<NavBoxIFace> getNavBoxes()
    {
        initialize();

        extendedNavBoxes.clear();
        extendedNavBoxes.addAll(navBoxes);

        RecordSetTask rsTask = (RecordSetTask)ContextMgr.getTaskByClass(RecordSetTask.class);

        extendedNavBoxes.addAll(rsTask.getNavBoxes());

        return extendedNavBoxes;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.core.BaseTask#getStarterPane()
     */
    public SubPaneIFace getStarterPane()
    {
        return new SimpleDescPane(title, this, "Please select an Interaction");
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.Taskable#getToolBarItems()
     */
    public List<ToolBarItemDesc> getToolBarItems()
    {
        Vector<ToolBarItemDesc> list = new Vector<ToolBarItemDesc>();
        String label = getResourceString(name);
        String iconName = name;
        String hint = getResourceString("interactions_hint");
        ToolBarDropDownBtn btn = createToolbarButton(label, iconName, hint);

        list.add(new ToolBarItemDesc(btn));

        return list;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.Taskable#getMenuItems()
     */
    public List<MenuItemDesc> getMenuItems()
    {
        Vector<MenuItemDesc> list = new Vector<MenuItemDesc>();
        return list;

    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.Taskable#getTaskClass()
     */
    public Class<? extends BaseTask> getTaskClass()
    {
        return this.getClass();
    }
    
    /**
     * Creates a new loan from a RecordSet.
     * @param recordSet the recordset to use to create the loan
     */
    protected void printLoan(final Object data)
    {
        //String loanNumber = null;
        if (data instanceof RecordSetIFace)
        {
            RecordSetIFace rs = (RecordSetIFace)data;
            
            // XXX For Demo purposes only we need to be able to look up report and labels
            final CommandAction cmd = new CommandAction(LabelsTask.LABELS, LabelsTask.PRINT_LABEL, rs);
            cmd.setProperty("file", "LoanInvoice.jrxml");
            cmd.setProperty("title", "Loan Invoice");
            cmd.setProperty(NavBoxAction.ORGINATING_TASK, this);
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    CommandDispatcher.dispatch(cmd);
                }
            });
        }
    }
    
    /**
     * Creates a new loan from a InfoRequest.
     * @param infoRequest the infoRequest to use to create the loan
     */
    protected void createNewLoan(final InfoRequest infoRequest)
    {   
        DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
        session.attach(infoRequest);
        RecordSetIFace rs = infoRequest.getRecordSet();
        session.close();   
        createNewLoan(rs);
    }
    
    /**
     * Creates a new loan from a RecordSet.
     * @param recordSet the recordset to use to create the loan
     */
    @SuppressWarnings("unchecked")
    protected void createNewLoan(final RecordSetIFace recordSet)
    {      
        DBTableIdMgr.getInClause(recordSet);

        DBTableIdMgr.TableInfo tableInfo = DBTableIdMgr.lookupInfoById(recordSet.getDbTableId());
        
        DataProviderFactory.getInstance().evict(tableInfo.getClassObj()); // XXX Not sure if this is really needed
        
        DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
        
        // First we process all the CollectionObjects in the RecordSet
        // and create a list of Preparations that can be loaned
        String sqlStr = DBTableIdMgr.getQueryForTable(recordSet);
        if (StringUtils.isNotBlank(sqlStr))
        {
            final LoanSelectPrepsDlg loanSelectPrepsDlg = new LoanSelectPrepsDlg((List<CollectionObject>)session.getDataList(sqlStr));
            loanSelectPrepsDlg.setModal(true);
            
            UIHelper.centerAndShow(loanSelectPrepsDlg);

            final Taskable thisTask = this;
            final Hashtable<Preparation, Integer> prepsHash = loanSelectPrepsDlg.getPreparationCounts();
            if (prepsHash.size() > 0)
            {
                final SwingWorker worker = new SwingWorker()
                {
                    public Object construct()
                    {
                        JStatusBar statusBar = (JStatusBar)UICacheManager.get(UICacheManager.STATUSBAR);
                        statusBar.setIndeterminate(true);
                        statusBar.setText(getResourceString("CreatingLoan"));
                        
                        Loan loan = new Loan();
                        loan.initialize();
                        
                        Calendar dueDate = Calendar.getInstance();
                        dueDate.add(Calendar.MONTH, 6);                 // XXX PREF Due Date
                        loan.setCurrentDueDate(dueDate);
                        
                        Shipment shipment = new Shipment();
                        shipment.initialize();
                        
                        loan.setShipment(shipment);
                        shipment.getLoans().add(loan);
                        
                        for (Preparation prep : prepsHash.keySet())
                        {
                            Integer count = prepsHash.get(prep);
                            
                            LoanPhysicalObject lpo = new LoanPhysicalObject();
                            lpo.initialize();
                            lpo.setPreparation(prep);
                            lpo.setQuantity(count.shortValue());
                            lpo.setLoan(loan);
                            loan.getLoanPhysicalObjects().add(lpo);
                        }
                        
                        DataEntryTask dataEntryTask = (DataEntryTask)TaskMgr.getTask(DataEntryTask.DATA_ENTRY);
                        if (dataEntryTask != null)
                        {
                            DBTableIdMgr.TableInfo loanTableInfo = DBTableIdMgr.lookupInfoById(loan.getTableId());
                            dataEntryTask.openView(thisTask, null, loanTableInfo.getDefaultFormName(), "edit", loan, true);
                        }
                        return null;
                    }

                    //Runs on the event-dispatching thread.
                    public void finished()
                    {
                        JStatusBar statusBar = (JStatusBar)UICacheManager.get(UICacheManager.STATUSBAR);
                        statusBar.setIndeterminate(false);
                        statusBar.setText("");
                    }
                };
                worker.start();
            }
            
        } else
        {
            log.error("Query String empty for RecordSet tableId["+recordSet.getDbTableId()+"]");
        }

    }
    
    /**
     * Fixes up the UI as to whether it is a new or existing loan and copies the 
     * LoanNumber to the ShipmentNumber.
     * @param formPane the form containing the loan
     */
    protected void adjustLoanForm(FormPane formPane)
    {
        FormViewObj formViewObj = formPane.getMultiView().getCurrentViewAsFormViewObj();
        if (formViewObj != null)
        {
            boolean     isNewObj    = MultiView.isOptionOn(formPane.getMultiView().getOptions(), MultiView.IS_NEW_OBJECT);

            Component comp = formViewObj.getControlByName("generateInvoice");
            if (comp instanceof JCheckBox)
            {
                //printLoan = ((JCheckBox)comp).isSelected();
            }
            comp = formViewObj.getControlByName("ReturnLoan");
            if (comp instanceof JButton)
            {
                comp.setVisible(!isNewObj);
            }
            comp = formViewObj.getControlByName("ReturnPartialLoan");
            if (comp instanceof JButton)
            {
                comp.setVisible(!isNewObj);
            }
            
            if (isNewObj)
            {
                comp = formViewObj.getControlByName("ReturnPartialLoan");
                if (comp instanceof JButton)
                {
                    comp.setVisible(!isNewObj);
                }
                Component shipComp = formViewObj.getControlByName("shipmentNumber");
                comp = formViewObj.getControlByName("loanNumber");
                if (comp instanceof JTextField && shipComp instanceof JTextField)
                {
                    JTextField loanTxt = (JTextField)comp;
                    JTextField shipTxt = (JTextField)shipComp;
                    shipTxt.setText(loanTxt.getText());
                }
                
                //Loan loan = (Loan)formPane.getData();
                //loan.getShipment().setShipmentNumber(loan.getLoanNumber());
            }

        }
    }
    
    /**
     * Creates a new InfoRequest from a RecordSet.
     * @param recordSet the recordset to use to create the InfoRequest
     */
    protected void createInfoRequest(final RecordSetIFace recordSet)
    {
        DBTableIdMgr.TableInfo tableInfo = DBTableIdMgr.lookupByShortClassName(InfoRequest.class.getSimpleName());
        
        SpecifyAppContextMgr appContextMgr = (SpecifyAppContextMgr)AppContextMgr.getInstance();
        
        View view = appContextMgr.getView(tableInfo.getDefaultFormName(), CollectionObjDef.getCurrentCollectionObjDef());

        InfoRequest infoRequest = new InfoRequest();
        infoRequest.initialize();
        infoRequest.setRecordSet(recordSet);
        
        createFormPanel(view.getViewSetName(), view.getName(), "edit", infoRequest, MultiView.IS_NEW_OBJECT);
        //recentFormPane.setIcon(IconManager.getIcon(INTERACTIONS, IconManager.IconSize.Std16));
    }

    
    /**
     * @param cmdAction
     */
    protected void checkToPrintLoan(final CommandAction cmdAction)
    {
        Loan loan = (Loan)cmdAction.getData();
        
        Boolean     printLoan   = null;
        FormViewObj formViewObj = getCurrentFormViewObj();
        if (formViewObj != null)
        {
            Component comp = formViewObj.getControlByName("generateInvoice");
            if (comp instanceof JCheckBox)
            {
                printLoan = ((JCheckBox)comp).isSelected();
            }
        }
        
        if (printLoan == null)
        {
            Object[] options = {getResourceString("CreateLoanInvoice"), getResourceString("Cancel")};
            int n = JOptionPane.showOptionDialog(UICacheManager.get(UICacheManager.FRAME),
                                                String.format(getResourceString("CreateLoanInvoiceForNum"), new Object[] {(loan.getLoanNumber())}),
                                                getResourceString("CreateLoanInvoice"),
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,     //don't use a custom Icon
                                                options,  //the titles of buttons
                                                options[0]); //default button title
            printLoan = n == 0;
        }
        
        if (printLoan)
        {
            RecordSet rs = new RecordSet();
            rs.initialize();
            rs.setName(loan.getIdentityTitle());
            rs.setDbTableId(loan.getTableId());
            rs.addItem(loan.getId());
            printLoan(rs);
        }
    }
    
    /**
     * Returns whether all the email prefs needed for sending mail have been filled in.
     * @return whether all the email prefs needed for sending mail have been filled in.
     */
    public boolean isEMailPrefsOK(final Hashtable<String, String> emailPrefs)
    {
        AppPreferences appPrefs = AppPreferences.getRemote();
        boolean allOK = true;
        String[] emailPrefNames = { "servername", "username", "password", "email"};
        for (String pName : emailPrefNames)
        {
            String key   = "settings.email."+pName;
            String value = appPrefs.get(key, "");
            //log.info("["+pName+"]["+value+"]");
            if (StringUtils.isNotEmpty(value) || pName.equals("password"))
            {
                emailPrefs.put(pName, value);
                
            } else
            {
                log.info("Key["+key+"] is empty");
                allOK = false;
                
                // XXX For Demo
                if (true)
                {
                    emailPrefs.put("servername", "imap.ku.edu");
                    emailPrefs.put("username", "rods");
                    emailPrefs.put("password", "");
                    emailPrefs.put("email", "rods@ku.edu");
                    allOK = true;
                }
                break;
            }
        }
        return allOK;
    }
    
    /**
     * Creates an Excel SpreadSheet or CVS file and attaches it to an email and send it to an agent.
     * 
     * @param infoRequest the info request to be sent
     */
    public void createAndSendEMail(final SubPaneIFace subPane)
    {
        FormViewObj formViewObj = getCurrentFormViewObj();
        if (formViewObj != null) // Should never happen
        {
            InfoRequest infoRequest = (InfoRequest)formViewObj.getDataObj();
            Agent       toAgent     = infoRequest.getAgent();
            
            boolean   sendEMail = true; // default to true
            Component comp      = formViewObj.getControlByName("sendEMail");
            if (comp instanceof JCheckBox)
            {
                sendEMail = ((JCheckBox)comp).isSelected();
            }
            
            MultiView mv = formViewObj.getSubView("InfoRequestColObj");
            if (mv != null && sendEMail)
            {
                final Viewable viewable = mv.getCurrentView();
                if (viewable instanceof TableViewObj)
                {
                    final Hashtable<String, String> emailPrefs = new Hashtable<String, String>();
                    if (!isEMailPrefsOK(emailPrefs))
                    {
                        JOptionPane.showMessageDialog(UICacheManager.get(UICacheManager.TOPFRAME), 
                                getResourceString("NO_EMAIL_PREF_INFO"), 
                                getResourceString("NO_EMAIL_PREF_INFO_TITLE"), JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    final File tempExcelFileName = TableModel2Excel.getTempExcelName();
                    
                    emailPrefs.put("to", toAgent.getEmail() != null ? toAgent.getEmail() : "");
                    emailPrefs.put("from", emailPrefs.get("email"));
                    emailPrefs.put("subject", String.format(getResourceString("INFO_REQUEST_SUBJECT"), new Object[] {infoRequest.getIdentityTitle()}));
                    emailPrefs.put("bodytext", "");
                    emailPrefs.put("attachedFileName", tempExcelFileName.getName());
                    
                    final Frame topFrame = (Frame)UICacheManager.get(UICacheManager.TOPFRAME);
                    final ViewBasedDisplayDialog dlg = new ViewBasedDisplayDialog(topFrame,
                                                  "SystemSetup",
                                                  "SendMail",
                                                  null,
                                                  getResourceString("SEND_MAIL_TITLE"),
                                                  getResourceString("SEND_BTN"),
                                                  null, // className,
                                                  null, // idFieldName,
                                                  true, // isEdit,
                                                  0);
                    dlg.setData(emailPrefs);
                    dlg.setModal(true);
                    
                    dlg.setCloseListener(new PropertyChangeListener()
                    {
                        public void propertyChange(PropertyChangeEvent evt)
                        {
                            String action = evt.getPropertyName();
                            if (action.equals("OK"))
                            {
                                dlg.getMultiView().getDataFromUI();
                                
                                System.out.println("["+emailPrefs.get("bodytext")+"]");
                                
                                TableViewObj  tblViewObj = (TableViewObj)viewable;
                                File          excelFile  = TableModel2Excel.convertToExcel(tempExcelFileName, 
                                                                                           getResourceString("CollectionObject"), 
                                                                                           tblViewObj.getTable().getModel());
                                StringBuilder sb         = TableModel2Excel.convertToHTML(getResourceString("CollectionObject"), 
                                                                                          tblViewObj.getTable().getModel());
                                
                                //EMailHelper.setDebugging(true);
                                String text = emailPrefs.get("bodytext").replace("\n", "<br>") + "<BR><BR>" + sb.toString();
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        UICacheManager.displayLocalizedStatusBarText("SENDING_EMAIL");
                                    }
                                });
                                
                                String password = Encryption.decrypt(emailPrefs.get("password"));
                                if (StringUtils.isEmpty(password))
                                {
                                    password = EMailHelper.askForPassword(topFrame);
                                }
                                
                                if (StringUtils.isNotEmpty(password))
                                {
                                    final boolean status = EMailHelper.sendMsg(emailPrefs.get("servername"), 
                                                                               emailPrefs.get("username"), 
                                                                               password, 
                                                                               emailPrefs.get("email"), 
                                                                               emailPrefs.get("to"), 
                                                                               emailPrefs.get("subject"), text, EMailHelper.HTML_TEXT, excelFile);
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run()
                                        {
                                            UICacheManager.displayLocalizedStatusBarText(status ? "EMAIL_SENT_ERROR" : "EMAIL_SENT_OK");
                                        }
                                    });
                                }
                            }
                            else if (action.equals("Cancel"))
                            {
                                log.warn("User clicked Cancel");
                            }
                        }
                    });
    
                    dlg.setVisible(true);
                }
            }
        } else
        {
            log.error("Why doesn't the current SubPane have a main FormViewObj?");
        }
    }
    
    /**
     * Delete a InfoRequest..
     * @param infoRequest the infoRequest to be deleted
     */
    protected void deleteInfoRequest(final InfoRequest infoRequest)
    {
        // delete from database
        DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
        try
        {
            // ???? session.attach(infoRequest);
            session.beginTransaction();
            session.delete(infoRequest);
            session.commit();
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            log.error(ex);
        }
        session.close();

    }
    
    /**
     * Delete the InfoRequest from the UI, which really means remove the NavBoxItemIFace. 
     * This method first checks to see if the boxItem is not null and uses that, if
     * it is null then it looks the box up by name ans used that
     * @param boxItem the box item to be deleted
     * @param infoRequest the infoRequest that is "owned" by some UI object that needs to be deleted (used for secodary lookup
     */
    protected void deleteInfoRequestFromUI(final NavBoxItemIFace boxItem, final InfoRequest infoRequest)
    {
        
        Component comp = boxItem != null ? boxItem.getUIComponent() : getBoxByTitle(infoRequest.getIdentityTitle()).getUIComponent(); 
        if (comp != null)
        {
            infoRequestNavBox.remove(comp);
            
            // XXX this is pathetic and needs to be generized
            infoRequestNavBox.invalidate();
            infoRequestNavBox.setSize(infoRequestNavBox.getPreferredSize());
            infoRequestNavBox.doLayout();
            infoRequestNavBox.repaint();
            NavBoxMgr.getInstance().invalidate();
            NavBoxMgr.getInstance().doLayout();
            NavBoxMgr.getInstance().repaint();
            UICacheManager.forceTopFrameRepaint();
        }
    }
    
    /**
     * Starts process to return a loan
     * @param doPartial true means show dialog and do partial, false means just return the loan
     */
    protected void doReturnLoan(final Loan   loan, 
                                final Agent  agent, 
                                final List<LoanReturnInfo> returns)
    {
        final SwingWorker worker = new SwingWorker()
        {
            public Object construct()
            {
                JStatusBar statusBar = (JStatusBar)UICacheManager.get(UICacheManager.STATUSBAR);
                statusBar.setIndeterminate(true);
                statusBar.setText(getResourceString("ReturningLoanItems"));
                
                for (LoanReturnInfo lri : returns)
                {   
                    LoanPhysicalObject       lpo  = lri.getLoanPhysicalObject();
                    LoanReturnPhysicalObject lrpo = new LoanReturnPhysicalObject();
                    lrpo.initialize();
                    lrpo.setAgent(agent);
                    lrpo.setLastEditedBy(FormHelper.getCurrentUserEditStr());
                    lrpo.setReturnedDate(Calendar.getInstance());
                    lrpo.setQuantity(lri.getQuantity());
                    lrpo.setRemarks(lri.getRemarks());
                    if (lri.isResolved() != null)
                    {
                        lri.getLoanPhysicalObject().setIsResolved(lri.isResolved());
                    }
                    lrpo.setLoanPhysicalObject(lpo);
                    lpo.getLoanReturnPhysicalObjects().add(lrpo);
                    
                }
                /*
                Shipment shipment = new Shipment();
                shipment.initialize();
                
                loan.setShipment(shipment);
                shipment.getLoans().add(loan);
                
                for (Preparation prep : prepsHash.keySet())
                {
                    Integer count = prepsHash.get(prep);
                    
                    LoanPhysicalObject lpo = new LoanPhysicalObject();
                    lpo.initialize();
                    lpo.setPreparation(prep);
                    lpo.setQuantity(count.shortValue());
                    lpo.setLoan(loan);
                    loan.getLoanPhysicalObjects().add(lpo);
                }
                
                DataEntryTask dataEntryTask = (DataEntryTask)TaskMgr.getTask(DataEntryTask.DATA_ENTRY);
                if (dataEntryTask != null)
                {
                    DBTableIdMgr.TableInfo loanTableInfo = DBTableIdMgr.lookupInfoById(loan.getTableId());
                    dataEntryTask.openView(thisTask, null, loanTableInfo.getDefaultFormName(), "edit", loan, true);
                }*/
                return null;
            }

            //Runs on the event-dispatching thread.
            public void finished()
            {
                JStatusBar statusBar = (JStatusBar)UICacheManager.get(UICacheManager.STATUSBAR);
                statusBar.setIndeterminate(false);
                statusBar.setText("");
            }
        };
        worker.start();
    }
    
    /**
     * Starts process to return a loan
     * @param doPartial true means show dialog and do partial, false means just return the loan
     */
    protected void returnLoan()
    {
        Loan loan = null;
        SubPaneIFace subPane = SubPaneMgr.getInstance().getCurrentSubPane();
        if (subPane != null)
        {
            MultiView mv = subPane.getMultiView();
            if (mv != null)
            {
                if (mv.getData() instanceof Loan)
                {
                    loan = (Loan)mv.getData();
                }
            }
        }
        
        if (loan != null)
        {
            LoanReturnDlg dlg = new LoanReturnDlg(loan);
            dlg.setModal(true);
            dlg.setVisible(true);
            dlg.dispose();
            
            List<LoanReturnInfo> returns = dlg.getLoanReturnInfo();
            if (returns.size() > 0)
            {
                doReturnLoan(loan, dlg.getAgent(), returns);
            }
            
        } else
        {
            // XXX Show some kind of error dialog
        }
    }
    
    //-------------------------------------------------------
    // CommandListener Interface
    //-------------------------------------------------------

    @SuppressWarnings("unchecked")
    public void doCommand(CommandAction cmdAction)
    {
        
        if (cmdAction.isType(DB_CMD_TYPE))
        {
            if (cmdAction.getData() instanceof InfoRequest)
            {
                if (cmdAction.isAction(INSERT_CMD_ACT) || cmdAction.isAction(UPDATE_CMD_ACT))
                {
                    //final CommandAction cm = cmdAction;
                    // Create Specify Application
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            //createAndSendEMail((InfoRequest)cm.getData());  
                            CommandDispatcher.dispatch(new CommandAction(INTERACTIONS, CREATE_MAILMSG, SubPaneMgr.getInstance().getCurrentSubPane()));
                        }
                    });
                    if (cmdAction.isAction(INSERT_CMD_ACT))
                    {
                        InfoRequest infoRequest = (InfoRequest)cmdAction.getData();
                        NavBoxItemIFace nbi = addNavBoxItem(infoRequestNavBox, infoRequest.getIdentityTitle(), INTERACTIONS, INTERACTIONS, "Delete", infoRequest);
                        setUpDraggable(nbi, new DataFlavor[]{Trash.TRASH_FLAVOR, INFOREQUEST_FLAVOR}, new NavBoxAction("", ""));
                    }
                }
            } else if (cmdAction.getData() instanceof Loan)
            {
                if (cmdAction.isAction(INSERT_CMD_ACT) || cmdAction.isAction(UPDATE_CMD_ACT))
                {
                   checkToPrintLoan(cmdAction);
                }
            }
            

        } else if (cmdAction.isType(DataEntryTask.DATA_ENTRY))
        {
            if (cmdAction.isAction(DataEntryTask.OPEN_VIEW))
            {
                adjustLoanForm((FormPane)cmdAction.getData());
            }
            
        } else if (cmdAction.isAction(CREATE_MAILMSG))
        {
            createAndSendEMail((SubPaneIFace)cmdAction.getData());
            
        } else if (cmdAction.isAction("NewInteraction"))
        {
            if (cmdAction.getData() instanceof RecordSetIFace)
            {
                addSubPaneToMgr(DataEntryTask.createFormFor(this, name, (RecordSetIFace)cmdAction.getData()));

            } else if (cmdAction.getData() instanceof Object[])
            {
                Object[] dataList = (Object[])cmdAction.getData();
                if (dataList.length != 3)
                {
                    View   view = (View)dataList[0];
                    String mode = (String)dataList[1];
                    String idStr = (String)dataList[2];
                    DataEntryTask.openView(this, view, mode, idStr);

                } else
                {
                    log.error("The Edit Command was sent with an object Array that was not 3 components!");
                }
            } else
            {
                log.error("The Edit Command was sent that didn't have data that was a RecordSet or an Object Array");
            }
          
        } else if (cmdAction.isAction(PRINT_LOAN))
        {
            if (cmdAction.getData() instanceof RecordSetIFace)
            {
                if (((RecordSetIFace)cmdAction.getData()).getDbTableId() != cmdAction.getTableId())
                {
                    JOptionPane.showMessageDialog(null, getResourceString("ERROR_RECORDSET_TABLEID"), getResourceString("Error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            printLoan(cmdAction.getData());
            
        } else if (cmdAction.isAction("CreateInfoRequest") && cmdAction.getData() instanceof RecordSet)
        {
            Object data = cmdAction.getData();
            if (data instanceof RecordSet)
            {
                createInfoRequest((RecordSetIFace)data);
            }
            
        } else if (cmdAction.isAction("ReturnLoan"))
        {
            returnLoan();
            
        } else if (cmdAction.isAction("Delete") && cmdAction.getData() instanceof InfoRequest)
        {
            InfoRequest inforRequest = (InfoRequest)cmdAction.getData();
            deleteInfoRequest(inforRequest);
            deleteInfoRequestFromUI(null, inforRequest);
            
        } else 
        {
            Object cmdData = cmdAction.getData();
            if (cmdData == null)
            {
                //LabelsTask.askForRecordSet(tableId)   
            }
            
            // These all assume there needs to be a recordsset
            if (cmdData != null)
            {
                if (cmdData instanceof RecordSetIFace)
                {
                    RecordSetIFace rs = (RecordSetIFace)cmdData;
                    
                    if (rs.getDbTableId() == colObjTableId)
                    {
                        if (cmdAction.isAction(NEW_LOAN))
                        {    
                            createNewLoan(rs);
                                
                        } else if (cmdAction.isAction(InfoRequestName))
                        {
                            createInfoRequest(rs);    
     
                        }
                    } else
                    {
                        log.error("Dropped wrong table type.");
                        // Error Msg Dialog XXX
                    }
                } else if (cmdData instanceof InfoRequest)
                {
                    createNewLoan((InfoRequest)cmdData);
                }
            }
        }
    }
}
