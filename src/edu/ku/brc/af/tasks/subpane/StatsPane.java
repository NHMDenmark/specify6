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

package edu.ku.brc.af.tasks.subpane;

import static edu.ku.brc.helpers.XMLHelper.getAttr;
import static edu.ku.brc.ui.UIHelper.createDuplicateJGoodiesDef;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.ku.brc.af.core.AppContextMgr;
import edu.ku.brc.af.core.Taskable;
import edu.ku.brc.af.core.expresssearch.QueryAdjusterForDomain;
import edu.ku.brc.dbsupport.JPAQuery;
import edu.ku.brc.specify.datamodel.DataModelObjBase;
import edu.ku.brc.stats.BarChartPanel;
import edu.ku.brc.stats.StatDataItem;
import edu.ku.brc.stats.StatGroupTable;
import edu.ku.brc.stats.StatGroupTableFromCustomQuery;
import edu.ku.brc.stats.StatGroupTableFromQuery;
import edu.ku.brc.stats.StatsMgr;
import edu.ku.brc.ui.CommandAction;

/**
 * A class that loads a page of statistics from an XML description
 
 * @code_status Complete
 **
 * @author rods
 *
 */
@SuppressWarnings("serial")
public class StatsPane extends BaseSubPane
{
    protected enum QueryType {SQL, JPA, CUSTOM}
    
    // Static Data Members
    private static final Logger log = Logger.getLogger(StatsPane.class);

    // Data Members
    protected String  resourceName       = null;
    protected Color   bgColor            = Color.WHITE;
    protected boolean useSeparatorTitles = false;

    protected int     PREFERREDWIDTH     = 300;
    protected int     SPACING            = 35;

    /**
     * Creates a StatsPane.
     * @param name name of pane
     * @param task the owning task
     * @param resourceName the name of the resource that contains the configuration
     * @param useSeparatorTitles indicates the group panels should use separator titles instead of boxes
     * @param bgColor the background color
    */
    public StatsPane(final String   name,
                     final Taskable task,
                     final String   resourceName,
                     final boolean  useSeparatorTitles,
                     final Color    bgColor)
    {
        super(name, task);

        this.resourceName = resourceName;
        this.useSeparatorTitles = useSeparatorTitles;

        if (bgColor != null)
        {
            this.bgColor = bgColor;
        } else
        {
            this.bgColor = Color.WHITE;
        }
        setBackground(this.bgColor);
        setLayout(new BorderLayout());

        init();
    }
    
    /**
     * Converts a string to a QueryType (default conversion is SQL)
     * @param type the string to be converted
     * @return the QueryType
     */
    protected QueryType getQueryType(final String type)
    {
        try
        {
            return QueryType.valueOf(type.toUpperCase());
        } catch (Exception ex)
        {
            log.error(ex);
        }
        return QueryType.SQL;
    }
    
    protected CommandAction createCommandActionFromElement(final Element command)
    {
        CommandAction cmdAction = null;
        if (command != null)
        {
            String typeStr   = getAttr(command, "type",  null);
            String actionStr = getAttr(command, "action", null);
            String className = getAttr(command, "class",  null);
            String data      = getAttr(command, "data",  null);
            
            
            
            if (StringUtils.isNotEmpty(typeStr) && 
                StringUtils.isNotEmpty(actionStr))
            {
                Class<? extends DataModelObjBase> classObj = null;
                
                if (StringUtils.isNotEmpty(className))
                {
                    try
                    {
                        classObj = Class.forName(className).asSubclass(DataModelObjBase.class);
                        
                    } catch (Exception ex)
                    {
                        
                    }
                    if (classObj != null)
                    {
                        cmdAction = new CommandAction(typeStr, actionStr, classObj);
                    }
                } else
                {
                    cmdAction = new CommandAction(typeStr, actionStr, data);
                }
            }
        }
        return cmdAction;
    }

    /**
     * Loads all the panels
     *
     */
    protected void init()
    {
        Element rootElement = null;
        try
        {
            rootElement = AppContextMgr.getInstance().getResourceAsDOM(resourceName);
            if (rootElement == null)
            {
                throw new RuntimeException("Couldn't find resource ["+resourceName+"]");
            }

            // count up rows and column
            StringBuilder rowsDef = new StringBuilder(128);

            List<?> rows = rootElement.selectNodes("/panel/row");
            int maxCols = 0;
            for (Object obj : rows)
            {
                Element rowElement = (Element)obj;
                List<?>    boxes   = rowElement.selectNodes("box");
                maxCols = Math.max(maxCols, boxes.size());
                if (rowsDef.length() > 0)
                {
                    rowsDef.append(",15dlu,");
                }
                rowsDef.append("top:p");
            }

            int preferredWidth = PREFERREDWIDTH;
            int spacing        = SPACING;

            FormLayout      formLayout = new FormLayout(createDuplicateJGoodiesDef("f:min("+preferredWidth+"px;p)", spacing+"px", maxCols), rowsDef.toString());
            PanelBuilder    builder    = new PanelBuilder(formLayout);
            CellConstraints cc         = new CellConstraints();

            int y = 1;
            for (Object obj : rows)
            {
                Element rowElement = (Element)obj;

                int x = 1;
                List<?> boxes = rowElement.selectNodes("box");
                for (Object bo : boxes)
                {
                    Element boxElement = (Element)bo;

                    String type = getAttr(boxElement, "type", "box");
                    int colSpan = getAttr(boxElement, "colspan", 1);

                    Component comp = null;
                    if (type.equalsIgnoreCase("bar chart"))
                    {
                        String statName = getAttr(boxElement, "name", null);

                        if (isNotEmpty(statName))
                        {
                            BarChartPanel bcp = (BarChartPanel)StatsMgr.createStatPane(statName);
                            int width = colSpan > 1 ? ((maxCols * preferredWidth) + ((maxCols-1) * spacing)) : preferredWidth;
                            // We start by assuming the chart will be square which is why we use
                            // preferredWidth as the height, and then we calculate the new width
                            bcp.setPreferredChartSize(width, preferredWidth);
                            comp = bcp;
                            //comp.setSize(new Dimension(preferredWidth, preferredWidth));
                            //comp.setPreferredSize(new Dimension(preferredWidth, preferredWidth));
                            //comp.invalidate();
                            //comp.doLayout();
                            //System.out.println(comp.getSize());
                            validate();
                            doLayout();
                            repaint();
                        }

                    } else // The default is "Box"
                    {
                        
                        /*Vector<BoxColumnInfo> colInfo = new Vector<BoxColumnInfo>();
                        for (Object colObj : boxElement.selectNodes("cols/col"))
                        {
                            Element colElement = (Element)colObj;
                            
                            BoxColumnInfo.Type disciplinee = BoxColumnInfo.Type.valueOf(getAttr(colElement, "type", (String)null));
                            
                            colInfo.add(new BoxColumnInfo(getAttr(colElement, "col", -1),
                                                          getAttr(colElement, "title", ""),
                                                          disciplinee));
                            
                        }*/
                        
                        int    descCol   = getAttr(boxElement, "desccol", -1);
                        int    valCol    = getAttr(boxElement, "valcol", -1);
                        String descTitle = getAttr(boxElement, "desctitle", " ");
                        String title     = getAttr(boxElement, "title", " ");
                        String noresults = getAttr(boxElement, "noresults", null);
                        
                        String[] colNames = null;
                        if (valCol != -1 && descCol == -1)
                        {
                            colNames = new String[] {getAttr(boxElement, "valtitle", " ")};
                            
                        } else if (descCol != -1 && valCol == -1 && StringUtils.isNotEmpty(descTitle))
                        {
                            colNames = new String[] {descTitle};
                            
                        } else
                        {
                            colNames = new String[] {descTitle,
                                                     getAttr(boxElement, "valtitle", " ")};
                        }

                        Element sqlElement = (Element)boxElement.selectSingleNode("sql");
                        if (valCol > -1 && sqlElement != null)
                        {
                            QueryType queryType = getQueryType(getAttr(sqlElement, "type", "sql"));
                            
                            Element       command   = (Element)boxElement.selectSingleNode("command");
                            int           colId     = -1;
                            CommandAction cmdAction = null;
                            
                            if (command != null)
                            {
                                colId     = getAttr(command, "colid", -1);
                               cmdAction = createCommandActionFromElement(command);
                            }
                            
                            //System.out.println("["+queryType+"]");
                            try
                            {
                                String sql =  QueryAdjusterForDomain.getInstance().adjustSQL(sqlElement.getText());
                                
                                switch (queryType)
                                {
                                    case SQL :
                                    {
                                        sql = QueryAdjusterForDomain.getInstance().adjustSQL(sql);
                                        StatGroupTableFromQuery group = new StatGroupTableFromQuery(title,
                                                                                colNames,
                                                                                sql,
                                                                                descCol,
                                                                                valCol,
                                                                                useSeparatorTitles,
                                                                                noresults);
                                        if (cmdAction != null)
                                        {
                                            group.setCommandAction(cmdAction, colId);
                                        }
                                        comp = group;
                                        group.relayout();
                                    } break;
                                    
                                    case JPA :
                                    {
                                        StatGroupTableFromCustomQuery group = new StatGroupTableFromCustomQuery(title,
                                                                                        colNames,
                                                                                        new JPAQuery(sql),
                                                                                        useSeparatorTitles,
                                                                                        noresults);
                                        if (cmdAction != null)
                                        {
                                            group.setCommandAction(cmdAction, colId);
                                        }
                                        comp = group;
                                        group.relayout();
                                    } break;
                                    
                                    case CUSTOM :
                                    {
                                        StatGroupTableFromCustomQuery group = new StatGroupTableFromCustomQuery(title,
                                                                                                colNames,
                                                                                                sql, // the name
                                                                                                useSeparatorTitles,
                                                                                                noresults);
                                        if (cmdAction != null)
                                        {
                                            group.setCommandAction(cmdAction, colId);
                                        }
                                        comp = group;
                                        group.relayout();
                                    } break;
                                    
                                } // switch
    
                                
                            } catch (Exception ex)
                            {
                                ex.printStackTrace();
                            }
                            //log.debug("After Relayout: "+group.getPreferredSize()+" "+group.getSize()+" "+group.getComponentCount());

                        } else
                        {
                            
                            List<?> items = boxElement.selectNodes("item");
                            StatGroupTable groupTable = new StatGroupTable(title,
                                                                           colNames,
                                                                           useSeparatorTitles, 
                                                                           items.size());
                            for (Object io : items)
                            {
                                Element itemElement = (Element)io;
                                String  itemTitle   = getAttr(itemElement, "title", "N/A");

                                String  formatStr  = null;
                                Element formatNode = (Element)itemElement.selectSingleNode("sql/format");
                                if (formatNode != null)
                                {
                                    formatStr = formatNode.getTextTrim();
                                }
                                
                                Element       command   = (Element)itemElement.selectSingleNode("command");
                                CommandAction cmdAction = null;
                                if (command != null)
                                {
                                    cmdAction = createCommandActionFromElement(command);
                                }

                                StatDataItem statItem       = new StatDataItem(itemTitle, cmdAction, getAttr(itemElement, "useprogress", false));
                                Element      subSqlElement  = (Element)itemElement.selectSingleNode("sql");
                                QueryType    queryType      = getQueryType(getAttr(subSqlElement, "type", "sql"));
                                
                                //System.out.println("["+queryType+"]");
                                switch (queryType)
                                {
                                    case SQL :
                                    {
                                        List<?> statements = itemElement.selectNodes("sql/statement");
                                        
                                        if (statements.size() == 1)
                                        {
                                            String sql = QueryAdjusterForDomain.getInstance().adjustSQL(((Element)statements.get(0)).getText());
                                            statItem.add(sql, 1, 1, StatDataItem.VALUE_TYPE.Value, formatStr);
        
                                        } else if (statements.size() > 0)
                                        {
                                            int cnt = 0;
                                            for (Object stObj : statements)
                                            {
                                                Element stElement = (Element)stObj;
                                                int    vRowInx = getAttr(stElement, "row", -1);
                                                int    vColInx = getAttr(stElement, "col", -1);
                                                String format  = getAttr(stElement, "format", null);
                                                String sql     = QueryAdjusterForDomain.getInstance().adjustSQL(stElement.getText());
                                                
                                                if (vRowInx == -1 || vColInx == -1)
                                                {
                                                    statItem.add(sql, format); // ignore return object
                                                } else
                                                {
                                                    statItem.add(sql, vRowInx, vColInx, StatDataItem.VALUE_TYPE.Value, format); // ignore return object
                                                }
                                                cnt++;
                                            }
                                        }
                                    } break;
                                    
                                    case JPA :
                                    {
                                        List<?> statements = itemElement.selectNodes("sql/statement");
                                        String sql = QueryAdjusterForDomain.getInstance().adjustSQL(((Element)statements.get(0)).getText());
                                        statItem.addCustomQuery(new JPAQuery(sql), formatStr);

                                    } break;
                                    
                                    case CUSTOM :
                                    {
                                        String subSqlName = getAttr(subSqlElement, "name", null);
                                        if (StringUtils.isNotEmpty(subSqlName))
                                        {
                                            statItem.addCustomQuery(subSqlName, formatStr);
                                            
                                        } else
                                        {
                                            log.error("Name is empty for box item ["+getAttr(itemElement, "title", "N/A")+"]");
                                        }
                                        
                                    } break;
                                }

                                groupTable.addDataItem(statItem);
                                statItem.startUp();

                            }
                            groupTable.relayout();
                            //log.debug(groupTable.getPreferredSize());
                            comp = groupTable;
                            //comp = scrollPane;
                        }

                    }

                    if (comp != null)
                    {
                        if (colSpan == 1)
                        {
                            builder.add(comp, cc.xy(x, y));

                        } else
                        {
                            builder.add(comp, cc.xywh(x, y, colSpan, 1));
                        }
                        x += 2;
                    }
                } // boxes
                y += 2;
            }

            setBackground(bgColor);

            JPanel statPanel = builder.getPanel();
            statPanel.setBackground(Color.WHITE);
            //statPanel.setOpaque(false);

            builder    = new PanelBuilder(new FormLayout("C:P:G", "p"));
            builder.add(statPanel, cc.xy(1,1));
            JPanel centerPanel = builder.getPanel();

            centerPanel.setBackground(Color.WHITE);
            //centerPanel.setOpaque(false);
            centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            add(centerPanel, BorderLayout.CENTER);

            centerPanel.validate();
            validate();
            doLayout();

        } catch (Exception ex)
        {
            log.error(ex);
            ex.printStackTrace();
        }

    }

}
