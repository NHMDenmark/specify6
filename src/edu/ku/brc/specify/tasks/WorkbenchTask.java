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

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;

import edu.ku.brc.af.core.MenuItemDesc;
import edu.ku.brc.af.core.NavBox;
import edu.ku.brc.af.core.NavBoxAction;
import edu.ku.brc.af.core.SubPaneIFace;
import edu.ku.brc.af.core.ToolBarItemDesc;
import edu.ku.brc.af.tasks.BaseTask;
import edu.ku.brc.specify.tasks.subpane.WorkbenchPane;
import edu.ku.brc.ui.CommandAction;
import edu.ku.brc.ui.CommandDispatcher;
import edu.ku.brc.ui.IconManager;
import edu.ku.brc.ui.ToolBarDropDownBtn;
import edu.ku.brc.ui.UICacheManager;

/**
 * Placeholder for additional work.
 *
 * @code_status Alpha
 *
 * @author meg
 *
 */
public class WorkbenchTask extends BaseTask
{
	private static final Logger log = Logger.getLogger(WorkbenchTask.class);
	public static final DataFlavor WORKBENCH_FLAVOR = new DataFlavor(WorkbenchTask.class, "Workbench");
    
    public static final String WORKBENCH = "Workbench";
    public static final String IMPORT_FIELD_NOTEBOOK = "Import Field Note Book";
    

    protected Vector<ToolBarDropDownBtn> tbList = new Vector<ToolBarDropDownBtn>();
    protected Vector<JComponent>          menus  = new Vector<JComponent>();

	/**
	 * Constructor. 
	 */
	public WorkbenchTask() 
    {
		super(WORKBENCH, getResourceString(WORKBENCH));
        
		CommandDispatcher.register(WORKBENCH, this);        
        CommandDispatcher.register(APP_CMD_TYPE, this);

	}

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.core.Taskable#initialize()
     */
    public void initialize()
    {
        if (!isInitialized)
        {
            super.initialize(); // sets isInitialized to false
            
            NavBox navBox = new NavBox(getResourceString("File"));
            navBox.add(NavBox.createBtn(getResourceString("New_Workbench"), name, IconManager.IconSize.Std16));
            navBox.add(NavBox.createBtn(getResourceString("New_Template"), name, IconManager.IconSize.Std16));
            navBoxes.addElement(navBox);
            //
            navBox = new NavBox(getResourceString("Templates"));
            navBox.add(NavBox.createBtn(getResourceString("Field_Book_Entry"),  name, IconManager.IconSize.Std16, new NavBoxAction(WORKBENCH, IMPORT_FIELD_NOTEBOOK)));
            navBox.add(NavBox.createBtn(getResourceString("Label_Entry"), name, IconManager.IconSize.Std16));
            navBoxes.addElement(navBox);

            navBox = new NavBox(getResourceString("Workbenches"));
            navBox.add(NavBox.createBtn(getResourceString("Lawrence_River"), name,IconManager.IconSize.Std16));
            navBox.add(NavBox.createBtn(getResourceString("Smith_Collection"), name, IconManager.IconSize.Std16));
            navBoxes.addElement(navBox);
        }
    }


    /**
     * IMports a Field Notebook. 
     */
    protected void importFieldNotebook()
    {
        if (false)
        {
    		JFileChooser jfc = new JFileChooser();
    		FileFilter filter = new FileFilter()
    		{
    			public boolean accept(File f)
    			{
    				if( f.getName().length() < 4 )
    				{
    					return false;
    				}
    				String nameEnd = f.getName().substring(f.getName().length()-4);
    				if( nameEnd.equalsIgnoreCase(".csv") || nameEnd.equalsIgnoreCase(".tab") )
    					return true;
    				return false;
    			}
    			public String getDescription()
    			{
    				return "csv files";
    			}
    		};
    		jfc.setFileFilter(filter);
    		int result = jfc.showOpenDialog(UICacheManager.get(UICacheManager.TOPFRAME));
    		if( result == JFileChooser.APPROVE_OPTION )
    		{
    			File csvFile = jfc.getSelectedFile();
    			log.debug("Importing field notebook: " + csvFile.getAbsolutePath() );
    			addSubPaneToMgr(new WorkbenchPane("Field Notebook Import", this, jfc.getSelectedFile()));
    		}
        } else
        {
            FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File f, String nameArg) {
                String fileName = nameArg;
                if( fileName.length() < 4 )
                {
                    return false;
                }
                String nameEnd = fileName.substring(fileName.length()-4);

                if( nameEnd.equalsIgnoreCase(".csv") || nameEnd.equalsIgnoreCase(".tab") )
                {
                    //System.out.println(name+"["+nameEnd+"]");
                    return true;
                }
                return false;
            }
            };
            FileDialog dialog = new FileDialog((Frame)UICacheManager.get(UICacheManager.TOPFRAME), "Import Data",FileDialog.LOAD);
            dialog.setFilenameFilter(filter);
            dialog.setVisible(true);
            log.info("Importing field notebook: " + dialog.getFile());


            String curFile;
            if ((curFile = dialog.getFile()) != null)
            {
                String filename = dialog.getDirectory() + curFile;

                addSubPaneToMgr(new WorkbenchPane("Field Notebook Import", this, new File(filename)));
            }

        }
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.BaseTask#getStarterPane()
     */
    public SubPaneIFace getStarterPane()
    {
        return starterPane = new WorkbenchPane(name, this,null);
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.plugins.Taskable#getToolBarItems()
     */
    public List<ToolBarItemDesc> getToolBarItems()
    {
//        Vector<ToolBarItemDesc> list = new Vector<ToolBarItemDesc>();
//        ToolBarDropDownBtn btn = createToolbarButton(name, "workbench.gif", "workbench_hint");
//        list.add(new ToolBarItemDesc(btn));
//        return list;
        Vector<ToolBarItemDesc> list = new Vector<ToolBarItemDesc>();
        String label = getResourceString(name);
        String iconName = name;
        String hint = getResourceString("workbench_hint");
        ToolBarDropDownBtn btn = createToolbarButton(label, iconName, hint);

        list.add(new ToolBarItemDesc(btn));
        return list;
//      return null;
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
    
    //-------------------------------------------------------
    // CommandListener Interface
    //-------------------------------------------------------
    
    /**
     * Processes all Commands of type DATA_ENTRY.
     * @param cmdAction the command to be processed
     */
    protected void processWorkbenchCommands(final CommandAction cmdAction)
    {
        
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.af.tasks.BaseTask#doCommand(edu.ku.brc.ui.CommandAction)
     */
    public void doCommand(CommandAction cmdAction)
    {
        if (cmdAction.isType(WORKBENCH))
        {
            processWorkbenchCommands(cmdAction);
            
        } else if (cmdAction.isType(APP_CMD_TYPE) && cmdAction.isAction(APP_RESTART_ACT))
        {
            //viewsNavBox.clear();
            //initializeViewsNavBox();
        }
    }
}
