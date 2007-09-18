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

import static edu.ku.brc.ui.UIRegistry.getResourceString;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import edu.ku.brc.af.core.ContextMgr;
import edu.ku.brc.af.core.ERTIJoinColInfo;
import edu.ku.brc.af.core.ExpressResultsTableInfo;
import edu.ku.brc.af.core.MenuItemDesc;
import edu.ku.brc.af.core.NavBoxIFace;
import edu.ku.brc.af.core.QueryForIdResultsHQL;
import edu.ku.brc.af.core.QueryForIdResultsSQL;
import edu.ku.brc.af.core.SubPaneIFace;
import edu.ku.brc.af.core.ToolBarItemDesc;
import edu.ku.brc.af.core.expresssearch.ExpressSearchConfigCache;
import edu.ku.brc.af.core.expresssearch.QueryForIdResultsIFace;
import edu.ku.brc.af.core.expresssearch.SearchConfig;
import edu.ku.brc.af.core.expresssearch.SearchConfigService;
import edu.ku.brc.af.core.expresssearch.SearchTableConfig;
import edu.ku.brc.af.prefs.AppPreferences;
import edu.ku.brc.af.tasks.BaseTask;
import edu.ku.brc.af.tasks.subpane.SimpleDescPane;
import edu.ku.brc.dbsupport.CustomQuery;
import edu.ku.brc.dbsupport.CustomQueryListener;
import edu.ku.brc.dbsupport.JPAQuery;
import edu.ku.brc.dbsupport.SQLExecutionListener;
import edu.ku.brc.dbsupport.SQLExecutionProcessor;
import edu.ku.brc.specify.tasks.subpane.ESResultsSubPane;
import edu.ku.brc.specify.tasks.subpane.ExpressSearchResultsPaneIFace;
import edu.ku.brc.specify.tasks.subpane.ExpressTableResultsFromQuery;
import edu.ku.brc.specify.ui.HelpMgr;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.CommandListener;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.SearchBox;
import edu.ku.brc.ui.UIHelper;
import edu.ku.brc.ui.UIRegistry;
import edu.ku.brc.ui.db.JAutoCompTextField;
import edu.ku.brc.ui.db.PickListDBAdapterFactory;
/**
 * This task will enable the user to index the database and preform express searches. This is where the Express Search starts.
 *
 * @code_status Complete
 *
 * @author rods
 *
 */
public class ExpressSearchTask extends BaseTask implements CommandListener, SQLExecutionListener, CustomQueryListener
{
    // Static Data Members
    private static final Logger log = Logger.getLogger(ExpressSearchTask.class);

    public static final String EXPRESSSEARCH      = "Express_Search";
    public static final String CHECK_INDEXER_PATH = "CheckIndexerPath";
    
    // Static Data Memebers
    protected static ExpressSearchTask               instance    = null;
    protected static final String                    LAST_SEARCH = "lastsearch"; 
    
    // Data Members
    protected SearchBox                    searchBox;
    protected JAutoCompTextField           searchText;
    protected JButton                      searchBtn;
    protected Color                        textBGColor    = null;
    protected Color                        badSearchColor = new Color(255,235,235);
    
    protected Vector<ESResultsSubPane> paneCache = new Vector<ESResultsSubPane>();
    
    protected Vector<SQLExecutionProcessor> sqlProcessorList = new Vector<SQLExecutionProcessor>();
    protected boolean                       sqlHasResults    = false;


    /**
     * Default Constructor.
     */
    public ExpressSearchTask()
    {
        super(EXPRESSSEARCH, getResourceString(EXPRESSSEARCH));
        icon = IconManager.getIcon("Search", IconManager.IconSize.Std16);
        
        closeOnLastPane = true;

        CommandDispatcher.register(APP_CMD_TYPE, this);
        CommandDispatcher.register(EXPRESSSEARCH, this);
        
        instance = this;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.core.Taskable#initialize()
     */
    public void initialize()
    {
        if (!isInitialized)
        {
            super.initialize(); // sets isInitialized to false
        }
    }
    
    /**
     * Returns true if the talk has been started and false if it hasn't.
     * @return true if the talk has been started and false if it hasn't.
     */
    public static boolean isStarted()
    {
        return instance != null;
    }

    
    /**
     * Returns whether there are file sin the lucence directory.
     * @return whether there are file sin the lucence directory.
     */
    public static boolean doesIndexExist()
    {
        return true; // XYZ
    }
    
    /**
     * Check to see of the index has been run and then enables the express search controls.
     *
     */
    public void checkForIndexer()
    {
        boolean exists = doesIndexExist();
        
        //log.debug(lucenePath.getAbsoluteFile() + " has index " + (lucenePath.list().length > 0));
        if (searchBtn != null)
        {
            searchBtn.setEnabled(exists);
            searchText.setEnabled(exists);
        }
    }

    /**
     * Performs the express search and returns the results.
     */
    protected void doQuery()
    {
        searchText.setBackground(textBGColor);
        String searchTerm = searchText.getText();
        if (isNotEmpty(searchTerm))
        {
            ESResultsSubPane expressSearchPane = new ESResultsSubPane(searchTerm, this, true);
            if (doQuery(searchText, badSearchColor, expressSearchPane))
            {
                addSubPaneToMgr(expressSearchPane);
            } else
            {
                UIRegistry.displayLocalizedStatusBarText("NoExpressSearchResults");
            }
            AppPreferences.getLocalPrefs().put(LAST_SEARCH, searchTerm);
        }
    }


    /**
     * Performs the express search and returns the results to the ExpressSearchResultsPaneIFace/
     * @param searchText the Text Control that contains the search string
     * @param badSearchColor the color to set the control if no results
     * @param tables ExpressResultsTableInfo hash
     * @param esrPane the pane that the results will be set into
     * @return true if results were found, false if not results
     */
    public static boolean doQuery(final JTextField searchText,
                                  final Color      badSearchColor,
                                  final ExpressSearchResultsPaneIFace esrPane)
    {
        return doQuery(searchText, null, badSearchColor, esrPane);
    }

    /**
     * Performs the express search and returns the results to the ExpressSearchResultsPaneIFace/
     * @param searchTextStr the string to use as the search
     * @param tables ExpressResultsTableInfo hash
     * @param esrPane the pane that the results will be set into
     * @return true if results were found, false if not results
     */
    public static boolean doQuery(final String     searchTextStr,
                                  final ExpressSearchResultsPaneIFace esrPane)
    {
        return doQuery(null, searchTextStr, null, esrPane);
    }
    
    /**
     * @param esrPane
     * @param searchTableConfig
     * @param searchTerm
     * @return
     */
    protected static SQLExecutionProcessor startSearch(final ExpressSearchResultsPaneIFace esrPane,
                                                       final SearchTableConfig searchTableConfig,
                                                       final String            searchTerm)
    {
        SQLExecutionProcessor sqlEP = null;
        String sqlStr = searchTableConfig.getSQL(searchTerm, true);
        if (sqlStr != null)
        {
            sqlEP = new SQLExecutionProcessor(instance, sqlStr);
            sqlEP.setData(new Object[] {searchTableConfig, esrPane, searchTerm});
            instance.sqlProcessorList.add(sqlEP);
            sqlEP.start();
        }
        return sqlEP;
    }
    
    /**
     * @param esrPane
     * @param searchTableConfig
     * @param searchTerm
     * @return
     */
    protected static JPAQuery startSearchJPA(final ExpressSearchResultsPaneIFace esrPane,
                                             final SearchTableConfig searchTableConfig,
                                             final String            searchTerm)
    {
        JPAQuery jpaQuery = null;
        String sqlStr = searchTableConfig.getSQL(searchTerm, true);
        if (sqlStr != null)
        {
            jpaQuery = new JPAQuery(instance, sqlStr);
            jpaQuery.setData(new Object[] {searchTableConfig, esrPane, searchTerm});
            jpaQuery.start();
        }
        return jpaQuery;
    }

    /**
     * Performs the express search and returns the results to the ExpressSearchResultsPaneIFace.
     * If the control is null then it will use the string.
     *
     * @param searchText the Text Control that contains the search string (can be null)
     * @param searchTextStr the Text Control that contains the search string (can be null)
     * @param badSearchColor the color to set the control if no results (can be null if searchText is null)
     * @param tables ExpressResultsTableInfo hash
     * @param esrPane the pane that the results will be set into
     * @return true if results were found, false if not results
     */
    public static boolean doQuery(final JTextField searchText,
                                  final String     searchTextStr,
                                  final Color      badSearchColor,
                                  final ExpressSearchResultsPaneIFace esrPane)
    {
        String searchTerm = searchText != null ? searchTerm = searchText.getText() : searchTextStr;

        boolean hasResults = true;
        if (searchTerm != null && searchTerm.length() > 0)
        {
            instance.sqlHasResults    = false;
            
            SearchConfig config = SearchConfigService.getInstance().getSearchConfig();
           
            for (SearchTableConfig table : config.getTables())
            {
                log.debug("**************> " +table.getTableName() );
                startSearchJPA(esrPane, table, searchTerm);
            }
            
            
        }
        
        if (!hasResults)
        {
            if (searchText != null)
            {
                if (badSearchColor != null)
                {
                    searchText.setBackground(badSearchColor);
                }
                searchText.setSelectionStart(0);
                searchText.setSelectionEnd(searchText.getText().length());
                searchText.getToolkit().beep();
            }
        }
        
        return hasResults;
    }
    
    /**
     * Traverses the individual result record ifs and maps them into the result tables.
     * @param searchIdStr the ID of the Express Search definition
     * @param recId the record Id
     * @param idToTableInfoMap the TableInfo mapped by ID
     * @param joinIdToTableInfoMap the TableInfo mapped by Join ID
     * @param resultsMap the primary result tables
     * @param resultsForJoinsMap the related results table
     */
    public static void collectResults(final QueryForIdResultsIFace qfir,
                                      final ResultSet              resultSet,
                                      final Integer                id,
                                      final Hashtable<String, List<ExpressResultsTableInfo>> joinIdToTableInfoMap,
                                      final Hashtable<String, QueryForIdResultsSQL>          resultsForJoinsMap)
    {
        try
        {
            Integer recId;
            if (resultSet != null)
            {
                recId = resultSet.getInt(1);
            } else
            {
                recId = id;
            }
            
            System.out.println("id "+id+"  "+recId);
            
            String tableIdAsStr = Integer.toString(qfir.getTableId());
            
            //log.debug("Find any Joins for TableID ["+tblInfo.getTableId()+"]");
            List<ExpressResultsTableInfo> list = joinIdToTableInfoMap.get(tableIdAsStr);
            if (list != null)
            {
                for (ExpressResultsTableInfo erti : list)
                {
                    //log.debug("Checking up["+tblInfo.getTableId()+"]");
                    QueryForIdResultsSQL results = resultsForJoinsMap.get(erti.getId());
                    if (results == null)
                    {
                        Integer joinColTableId = null;
                        ERTIJoinColInfo joinCols[] = erti.getJoins();
                        if (joinCols != null)
                        {
                            for (ERTIJoinColInfo jci :  joinCols)
                            {
                                if (tableIdAsStr.equals(jci.getJoinTableId()))
                                {
                                    joinColTableId = jci.getJoinTableIdAsInt();
                                    break;
                                }
                            }
                        }
                        if (joinColTableId == null)
                        {
                            throw new RuntimeException("Shouldn't have got here!");
                        }
                        Integer displayOrder = SearchConfigService.getInstance().getSearchConfig().getOrderForRelatedQueryId(erti.getId());
                        log.debug("ExpressSearchResults erti.getId()["+erti.getId()+"] joinColTableId["+joinColTableId+"] displayOrder["+displayOrder+"]");
                        if (erti.getId().equals("3"))
                        {
                            int x= 0;
                            x++;
                        }
                        results = new QueryForIdResultsSQL(erti.getId(), joinColTableId, erti, displayOrder, qfir.getSearchTerm());
                        resultsForJoinsMap.put(erti.getId(), results);
                    }
                    results.add(recId);
                }
            }
        } catch (SQLException ex)
        {
            ex.printStackTrace();
        }
    }
    
    
    /**
     * Traverses through the results and adds to the panel to be displayed.
     * @param resultsMap the primary result tables
     * @param resultsForJoinsMap the related results table
     */
    protected static void displayResults(final ExpressSearchResultsPaneIFace           esrPane,
                                         final QueryForIdResultsIFace                  queryResults,
                                         final Hashtable<String, QueryForIdResultsSQL> resultsForJoinsMap)
    {
        // For Debug Only
        if (false)
        {
            for (Enumeration<QueryForIdResultsSQL> e=resultsForJoinsMap.elements();e.hasMoreElements();)
            {
                QueryForIdResultsSQL rs = e.nextElement();
                if (rs.getRecIds().size() > 0)
                {
                    log.debug("\n\n------------------------------------");
                    log.debug("------------------------------------");
                    log.debug("Search Id "+rs.getTableInfo().getId() + 
                                       " Table Id "+rs.getTableInfo().getTableId() + 
                                       " Column Name "+rs.getJoinColTableId());
                    log.debug("------------------------------------");
                    for (Integer l : rs.getRecIds())
                    {
                        log.debug(l+" ");
                    }
                }
            }
        }
        
        if (queryResults.getRecIds().size() > 0)//|| tableInfo.getNumIndexes() > 0)
        {
            esrPane.addSearchResults(queryResults);
        }
        
        for (Enumeration<QueryForIdResultsSQL> e=resultsForJoinsMap.elements();e.hasMoreElements();)
        {
            QueryForIdResultsSQL rs = e.nextElement();
            if (rs.getRecIds().size() > 0)
            {
                esrPane.addSearchResults(rs);
            }
        }
        resultsForJoinsMap.clear();
    }

    /**
     * Executes a Local query directly against the Lucene index that is locally on disk.
     * @param lucenePath the Path to the Lucene Directory
     * @param analyzer the analyzer to use
     * @param searchTextStr the search string to be searched
     * @param esrPane the desintation panel of the results
     * @return true if OK
     */
    public void doBasicSearch(final String searchName)
    {
        
        Hashtable<String, ExpressResultsTableInfo> idToTableInfoMap = ExpressSearchConfigCache.getSearchIdToTableInfoHash();
        for (ExpressResultsTableInfo erti : idToTableInfoMap.values())
        {
            if (erti.getName().equals(searchName))
            {
                // This needs to be fixed in that it might not return any results
                // and we are always adding the pane.
                ESResultsSubPane     expressSearchPane = new ESResultsSubPane(erti.getTitle(), this, true);
                QueryForIdResultsSQL         esr               = new QueryForIdResultsSQL(erti.getTitle(), null, erti, 0, "");
                @SuppressWarnings("unused")
                ExpressTableResultsFromQuery esrfq             = new ExpressTableResultsFromQuery(expressSearchPane, esr, true);
                addSubPaneToMgr(expressSearchPane);
                return;
            }
        }
        log.error("Can't find a search definition for name ["+searchName+"]");
    }


    //-------------------------------------------------------
    // Taskable
    //-------------------------------------------------------

    /* (non-Javadoc)
     * @see edu.ku.brc.af.core.BaseTask#getStarterPane()
     */
    @Override
    public SubPaneIFace getStarterPane()
    {
        return starterPane = new SimpleDescPane(name, this, "This is the Express Search Pane");
    }
    
    /*
     *  (non-Javadoc)
     * @see edu.ku.brc.af.core.Taskable#getNavBoxes()
     */
    public java.util.List<NavBoxIFace> getNavBoxes()
    {
        initialize();

        Vector<NavBoxIFace>     extendedNavBoxes = new Vector<NavBoxIFace>();

        extendedNavBoxes.clear();
        extendedNavBoxes.addAll(navBoxes);

        RecordSetTask rsTask = (RecordSetTask)ContextMgr.getTaskByClass(RecordSetTask.class);

        extendedNavBoxes.addAll(rsTask.getNavBoxes());

        return extendedNavBoxes;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.core.Taskable#getToolBarItems()
     */
    @Override
    public List<ToolBarItemDesc> getToolBarItems()
    {
        Vector<ToolBarItemDesc> list = new Vector<ToolBarItemDesc>();

        // Create Search Panel
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        JPanel     searchPanel = new JPanel(gridbag);
        JLabel     spacer      = new JLabel("  ");

        searchBtn = new JButton(getResourceString("Search"));
        searchBtn.setToolTipText(getResourceString("ExpressSearchTT"));
        HelpMgr.setHelpID(searchBtn, "Express_Search");
        
        //searchText  = new JTextField("[19510707 TO 19510711]", 10);//"beanii"
        //searchText  = new JTextField("beanii", 15);
                
        searchText = new JAutoCompTextField(15, PickListDBAdapterFactory.getInstance().create("ExpressSearch", true));
        searchText.setAskBeforeSave(false);
        HelpMgr.setHelpID(searchText, "Express_Search");
        
        searchBox = new SearchBox(searchText, null);
        
        AppPreferences localPrefs = AppPreferences.getLocalPrefs();
        searchText.setText(localPrefs.get(LAST_SEARCH, ""));
        textBGColor = searchText.getBackground();

        //searchText.setMinimumSize(new Dimension(50, searchText.getPreferredSize().height));

        ActionListener doQuery = new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                doQuery();
            }
        };

        searchBtn.addActionListener(doQuery);
        searchText.addActionListener(doQuery);
        searchText.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                if (searchText.getBackground() != textBGColor)
                {
                    searchText.setBackground(textBGColor);
                }
            }
        });

        c.weightx = 1.0;
        gridbag.setConstraints(spacer, c);
        searchPanel.add(spacer);

        c.weightx = 0.0;
        gridbag.setConstraints(searchBox, c);
        searchPanel.add(searchBox);

        searchPanel.add(spacer);
        
        if (!UIHelper.isMacOS())
        {
            gridbag.setConstraints(searchBtn, c);
            searchPanel.add(searchBtn);
        }

        list.add(new ToolBarItemDesc(searchPanel, ToolBarItemDesc.Position.AdjustRightLastComp));

        checkForIndexer();

        return list;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#getMenuItems()
     */
    @Override
    public List<MenuItemDesc> getMenuItems()
    {
        return new Vector<MenuItemDesc>();
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#getTaskClass()
     */
    @Override
    public Class<? extends BaseTask> getTaskClass()
    {
        return this.getClass();
    }
    
    //----------------------------------------------------------------
    //-- CommandListener Interface
    //----------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#doCommand(edu.ku.brc.ui.CommandAction)
     */
    @Override
    public void doCommand(CommandAction cmdAction)
    {
        if (cmdAction.isType(APP_CMD_TYPE))
        {
            if (cmdAction.isAction(APP_RESTART_ACT))
            {
                checkForIndexer();
            }
            
        } else if (cmdAction.isType(EXPRESSSEARCH))
        {
            if (cmdAction.isAction(CHECK_INDEXER_PATH))
            {
                checkForIndexer();
                
            } else if (cmdAction.isAction("ExpressSearch"))
            {
                String searchTerm = cmdAction.getData().toString();
                ESResultsSubPane expressSearchPane = new ESResultsSubPane(searchTerm, this, true);
                if (doQuery(null, searchTerm, badSearchColor, expressSearchPane))
                {
                    addSubPaneToMgr(expressSearchPane);
                } else
                {
                    UIRegistry.displayLocalizedStatusBarText("NoExpressSearchResults");
                }
            } else if (cmdAction.isAction("Search"))
            {
                doBasicSearch(cmdAction.getData().toString());
            }  
        }
    }
    
    //------------------------------------------------
    //-- ExpressSearchIndexerListener
    //------------------------------------------------

   /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.subpane.ExpressSearchIndexerPane.ExpressSearchIndexerListener#doneIndexing()
     */
    public void doneIndexing()
    {
        checkForIndexer();
    }
    
    //-------------------------------------------------------------
    // SQLExecutionListener Interface
    //-------------------------------------------------------------
    /* (non-Javadoc)
     * @see edu.ku.brc.dbsupport.SQLExecutionListener#exectionDone(edu.ku.brc.dbsupport.SQLExecutionProcessor, java.sql.ResultSet)
     */
    @Override
    public void exectionDone(SQLExecutionProcessor process, ResultSet resultSet)
    {
        if (!sqlHasResults)
        {
            try
            {
                sqlHasResults = resultSet.first();
                
            } catch (SQLException ex)
            {
                ex.printStackTrace();
            }
        }
        sqlProcessorList.remove(process);
        
        Object[]                      data              = (Object[])process.getData();
        SearchTableConfig             searchTableConfig = (SearchTableConfig)data[0];
        ExpressSearchResultsPaneIFace esrPane           = (ExpressSearchResultsPaneIFace)data[1];
        String                        searchTerm        = (String)data[2];
        
        Hashtable<String, ExpressResultsTableInfo>       idToTableInfoMap     = ExpressSearchConfigCache.getSearchIdToTableInfoHash();
        Hashtable<String, List<ExpressResultsTableInfo>> joinIdToTableInfoMap = ExpressSearchConfigCache.getJoinIdToTableInfoHash();

        Hashtable<String, QueryForIdResultsSQL> resultsForJoinsMap = new Hashtable<String, QueryForIdResultsSQL>();
        
        try
        {
            if (resultSet.first())
            {
                String                  searchIdStr = Integer.toString(searchTableConfig.getTableInfo().getTableId());
                ExpressResultsTableInfo tblInfo = idToTableInfoMap.get(searchIdStr);
                if (tblInfo == null)
                {
                    throw new RuntimeException("Bad id from search["+searchIdStr+"]");
                }
                QueryForIdResultsSQL queryResults = new QueryForIdResultsSQL(searchIdStr, null, tblInfo, searchTableConfig.getDisplayOrder(), searchTerm);
                do
                {
                    collectResults(queryResults, resultSet, null, joinIdToTableInfoMap, resultsForJoinsMap);
                    
                } while(resultSet.next());
                
                displayResults(esrPane, queryResults, resultsForJoinsMap);
            }
        } catch (SQLException ex)
        {
            ex.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.dbsupport.SQLExecutionListener#executionError(edu.ku.brc.dbsupport.SQLExecutionProcessor, java.lang.Exception)
     */
    @Override
    public void executionError(SQLExecutionProcessor process, Exception ex)
    {
        sqlProcessorList.remove(process);
        
    }
    
    //-------------------------------------------------------------------------
    //-- CustomQueryListener Interface
    //-------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see edu.ku.brc.dbsupport.CustomQueryListener#exectionDone(edu.ku.brc.dbsupport.CustomQuery)
     */
    @Override
    public void exectionDone(final CustomQuery customQuery)
    {
        JPAQuery jpaQuery = (JPAQuery)customQuery;
        List<?> list      = jpaQuery.getDataObjects();
        if (!sqlHasResults)
        {
            sqlHasResults = !jpaQuery.isInError() && list != null && list.size() > 0;
        }
        
        if (list != null)
        {
            Object[]                      data              = (Object[])jpaQuery.getData();
            SearchTableConfig             searchTableConfig = (SearchTableConfig)data[0];
            ExpressSearchResultsPaneIFace esrPane           = (ExpressSearchResultsPaneIFace)data[1];
            String                        searchTerm        = (String)data[2];
    
            Hashtable<String, List<ExpressResultsTableInfo>> joinIdToTableInfoMap = ExpressSearchConfigCache.getJoinIdToTableInfoHash();
            
            Hashtable<String, QueryForIdResultsSQL> resultsForJoinsMap = new Hashtable<String, QueryForIdResultsSQL>();
            
            //log.debug("TID["+searchTableConfig.getTableInfo().getTableId() + "] Table Order ["+searchTableConfig.getDisplayOrder()+"] "+list.size());
            QueryForIdResultsHQL results = new QueryForIdResultsHQL(searchTableConfig, new Color(30, 144, 255), searchTerm, list);
            
            for (Object idObj : list)
            {
                Integer id = (Integer)idObj;
                collectResults(results, null, id, joinIdToTableInfoMap, resultsForJoinsMap);
                    
            }
            displayResults(esrPane, results, resultsForJoinsMap);
            
        } else
        {
            log.error("List was null and cant't be.");
        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.dbsupport.CustomQueryListener#executionError(edu.ku.brc.dbsupport.CustomQuery)
     */
    @Override
    public void executionError(final CustomQuery customQuery)
    {
        
    }
}
