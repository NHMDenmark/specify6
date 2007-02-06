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
package edu.ku.brc.af.core;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.ku.brc.af.tasks.subpane.SimpleDescPane;
import edu.ku.brc.dbsupport.RecordSetIFace;
import edu.ku.brc.ui.ExtendedTabbedPane;

/**
 * Manages all the SubPanes that are in the main Tabbed pane. It notifies listeners when SubPanes are added, removed or Shown.
 *
 * @code_status Complete
 *
 * @author rods
 *
 */
@SuppressWarnings("serial")
public class SubPaneMgr extends ExtendedTabbedPane implements ChangeListener
{
    private static final SubPaneMgr instance = new SubPaneMgr();

    protected enum NotificationType {Added, Removed, Shown}

    // Data Members
    protected Hashtable<String, SubPaneIFace> panes = new Hashtable<String, SubPaneIFace>();
    protected SubPaneIFace currentPane = null;

    protected List<SubPaneMgrListener> listeners = new ArrayList<SubPaneMgrListener>();

    /**
     * Singleton Constructor.
     *
     */
    protected SubPaneMgr()
    {
        // This way we notifications that the tabs have changed
        addChangeListener(this);

        setOpaque(true); // this is so the tabs are painted correctly against the BG color of the TabbedPane
    }

    /**
     * Returns the reference to the singleton.
     * @return the reference to the singleton
     */
    public static SubPaneMgr getInstance()
    {
        return instance;
    }
    
    /**
     * Returns a unique tab name based on the generic tab name.
     * @param paneName the "generic" name of the panel (without the "()")
     * @return the new unique name with possibly parenthises
     */
    protected String buildUniqueName(final String paneName)
    {
        String title = paneName;
        boolean nameInUse = (panes.get(paneName) != null) ? true : false;
        int     index     = 2;
        while (nameInUse)
        {
            title = paneName + "("+index+")";
            nameInUse = (panes.get(title) != null) ? true : false;
            index++;
        }
        return title;
    }

    /**
     * Adds the sub pane and return the same one it added. It's ok to add the same
     * pane in twice, it will detect it, show it, and then return.
     * @param pane the pane to be added
     * @return the same pane
     */
    public SubPaneIFace addPane(final SubPaneIFace pane)
    {
        if (pane == null)
        {
            throw new NullPointerException("Null name or pane when adding to SubPaneMgr");
        }
        
        if (instance.panes.contains(pane))
        {
            showPane(pane);
            return pane;
        }
        /*
        for (Enumeration<SubPaneIFace> e=instance.panes.elements();e.hasMoreElements();)
        {
            if (e.nextElement() == pane)
            {
                showPane(pane);
                return pane;
            }
        }*/

        // Add this pane to the tabs
        String title = buildUniqueName(pane.getName());
        pane.setName(title);

        panes.put(title, pane); // this must be done before adding it
        addTab(title, pane.getIcon(), pane.getUIComponent());
        
        notifyListeners(NotificationType.Added, pane);

        this.setSelectedIndex(getTabCount()-1);

        // XXX Are these needed??
        /*SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                pane.getUIComponent().invalidate();
                invalidate();
                doLayout();
            }
        });*/
        return pane;
    }
    
    /**
     * Renames the pane, internally and the tab.
     * @param pane the pane to be renamed
     * @param newName the new name for the tab
     * @return the same pane as the one renamed
     */
    public SubPaneIFace renamePane(final SubPaneIFace pane, final String newName)
    {
        panes.remove(pane.getName());
        pane.setName(newName);
        this.setTitleAt(indexOfComponent(pane.getUIComponent()), newName);
        panes.put(newName, pane);
        
        return pane;
    }

    /**
     * Replaces  the "old pane" with a new pane, the main point here that the new gets inserted into the same position.
     * @param oldPane the old pane to be replaced
     * @param newName the new name for the tab
     * @return the same pane as the one renamed
     */
    public SubPaneIFace replacePane(final SubPaneIFace oldPane, final SubPaneIFace newPane)
    {
        //System.err.println("SubPaneMgr::replacePane ************************************************");

        // The caller is assuming that the pane is there to be replaced, but the SubPaneMgr can't
        // be responsible for that. So if the pane to be replaced is not found then it just adds 
        // SubPane straight away.
        SubPaneIFace fndPane = panes.get(oldPane.getName());
        if (oldPane != fndPane)
        {
            //log.warn("Couldn't find Pane ["+oldPane.getName()+"]");
            addPane(newPane);
            return newPane;
        }
        
        // Add this pane to the tabs
        String title = buildUniqueName(newPane.getName());
        newPane.setName(title);
        
        int index = this.indexOfComponent(oldPane.getUIComponent());
        panes.remove(oldPane.getName());
        this.remove(index);
        panes.put(newPane.getName(), newPane);
        
        this.insertTab(newPane.getName(), newPane.getIcon(), newPane.getUIComponent(), null, index);
        this.setSelectedIndex(index);
        return newPane;
    }


    /**
     * Removes a pane and calls shutdown on it.
     * @param pane the pane to be removed
     * @return the same pane as the one removed
     */
    public SubPaneIFace removePane(final SubPaneIFace pane)
    {
        if (currentPane == pane)
        {
            pane.showingPane(false);
            currentPane = null;
        }
        notifyListeners(NotificationType.Removed, pane);
        this.remove(pane.getUIComponent());
        panes.remove(pane.getName());
        pane.shutdown();
        return pane;
    }

    /**
     * Remove all the SubPanes
     */
    public void removeAllPanes()
    {
        // Move all the elements to a List so the iterator on the Hashtable works correctly
        List<SubPaneIFace> list = new ArrayList<SubPaneIFace>(panes.size());
        for (Enumeration<SubPaneIFace> e=panes.elements();e.hasMoreElements();)
        {
            list.add(e.nextElement());
        }
        
        for (SubPaneIFace sp : list)
        {
            removePane(sp);
        }

        list.clear(); // not really necessary
        
        // Make Sure
        removeAll();
        panes.clear();
    }
    
    /**
     * Looks for the first SubPane it finds that is has the same task  and the UI is of SimpleDescPane and then replaces it.
     * If there are none then it adds it.
     * @param subPane the new subpane
     */
    public static void replaceSimplePaneForTask(final SubPaneIFace subPane)
    {
        for (Enumeration<SubPaneIFace> e=instance.panes.elements();e.hasMoreElements();)
        {
            SubPaneIFace sp = e.nextElement();
            if (sp.getTask() == subPane.getTask() && sp.getUIComponent() instanceof SimpleDescPane)
            {
                instance.replacePane(sp, subPane);
                return;
            }
        }
        instance.addPane(subPane);
    }

    /**
     * Returns a SubPaneIFace that currently contains the same recordset as the one in question.
     * @param rs the recordset in question
     * @return the subpane containing the recordset or null.
     */
    public static SubPaneIFace getSubPaneWithRecordSet(final RecordSetIFace rs)
    {
        SubPaneIFace subPane = null;
        if (rs != null)
        {
            for (Enumeration<SubPaneIFace> e=instance.panes.elements();e.hasMoreElements();)
            {
                SubPaneIFace sp = e.nextElement();
                RecordSetIFace sprs = sp.getRecordSet();
                if (sprs == rs || 
                   (sprs != null && sprs.getRecordSetId() != null && 
                                    rs.getRecordSetId() != null &&
                                    sprs.getRecordSetId().intValue() == sprs.getRecordSetId().intValue()))
                {
                    return sp;
                }
            }
        }
        return subPane;
    }
    
    /**
     * Returns a list of the SubPaneIFaces.
     * @return a list of the SubPaneIFaces.
     */
    public Collection<SubPaneIFace> getSubPanes()
    {
        return panes.values();
    }

    /**
     * Tell Each SubPane that it is about to be shutdown, if the SubPane passes back false then the shutdown stops.
     */
    public boolean aboutToShutdown()
    {
        // Move all the elements to a List so the iterator on the Hashtable works correctly
        // if the a SubPane wants to remove itself
        List<SubPaneIFace> list = new ArrayList<SubPaneIFace>(panes.size());
        for (Enumeration<SubPaneIFace> e=panes.elements();e.hasMoreElements();)
        {
            list.add(e.nextElement());
        }
        
        for (SubPaneIFace sp : list)
        {
            boolean ok = sp.aboutToShutdown();
            if (!ok)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Show (makes visible) the pane by name.
     * @param name the name of the pane to be shown
     * @return the pane that is now shown
      */
    public SubPaneIFace showPane(final String name)
    {
        // Look the the desired pane
        SubPaneIFace pane = panes.get(name);
        return showPane(pane);
    }
    
    /**
     * Shows a SubPane
     * @param pane the SubPane to be shown
     * @return the SubPane that was passed in
     */
    public SubPaneIFace showPane(SubPaneIFace pane)
    {
        if (pane != null)
        {
            // Notify the current pane it is about to be hidden
            SubPaneIFace oldPane = getCurrentSubPane();
            if (oldPane != null && oldPane != pane)
            {
                oldPane.showingPane(false);
            }

            // Notify the new pane it is about to be show
            pane.showingPane(true);
            this.setSelectedComponent(pane.getUIComponent());
        } else
        {
            throw new NullPointerException();
        }
        return pane;
    }

    /**
     * Returns a SubPane for the UI component that it represents.
     * @param comp the component to be looked up
     * @return Returns a SubPane for the UI component that it represents
     */
    public SubPaneIFace getSubPaneForComponent(final Component comp)
    {
        for (Enumeration<SubPaneIFace> e=panes.elements();e.hasMoreElements();)
        {
            SubPaneIFace sp = e.nextElement();
            if (sp.getUIComponent() == comp)
            {
                return sp;
            }
        }
        return null;
    }

    /**
     * Returns the current sub pane.
     * @return the current sub pane.
     */
    public SubPaneIFace getCurrentSubPane()
    {
        return getSubPaneForComponent(getComponentAt(this.getSelectedIndex()));
    }

    /**
     * Returns a sub pane at an index.
     * @param index the indes of the sub pane
     * @return Returns a sub pane at an index
     */
    public SubPaneIFace getSubPaneAt(final int index)
    {
        return getSubPaneForComponent(getComponentAt(index));
    }

    /**
     * Removes all the Tabs.
     *
     */
    public void closeAll()
    {
        SubPaneIFace subPane = this.getCurrentSubPane();
        if (subPane != null)
        {
            for (Enumeration<SubPaneIFace> e=panes.elements();e.hasMoreElements();)
            {
                SubPaneIFace sp = e.nextElement();
                sp.showingPane(false); // Not sure about this notification
                notifyListeners(NotificationType.Removed, sp);
            }
            panes.clear();
        }
        this.removeAll();
    }

    /**
     * Removes the current tab.
     */
    @Override
    public void closeCurrent()
    {
        SubPaneIFace subPane = this.getCurrentSubPane();
        if(subPane.aboutToShutdown())
        {
        	this.removePane(subPane);
        }
    }

    /**
     * Adds listener of changes (adds and removes) of SubPaneIFaces to the manager.
     * @param l the listener
     */
    public void addListener(final SubPaneMgrListener l)
    {
        listeners.add(l);
    }

    /**
     * Removes listener.
     * @param l the listener
     */
    public void removeListener(final SubPaneMgrListener l)
    {
        listeners.remove(l);
    }

    /**
     * Notifies listeners when something happens to a subpane.
     * @param type the type of notification
     * @param subPane the subpane it happened to
     */
    protected void notifyListeners(final NotificationType type, final SubPaneIFace subPane)
    {
        for (SubPaneMgrListener l : listeners)
        {
            if (type == NotificationType.Added)
            {
                l.subPaneAdded(subPane);
            } else if (type == NotificationType.Removed)
            {
                l.subPaneRemoved(subPane);
            } else if (type == NotificationType.Shown)
            {
                l.subPaneShown(subPane);
            }
        }
    }


    //--------------------------------------------------------
    // ChangeListener
    //--------------------------------------------------------

    /* (non-Javadoc)
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent e)
    {
        int index = getSelectedIndex();
        if (index > -1)
        {
            SubPaneIFace subPane = getSubPaneAt(index);
            // might be null when it is the very first one
            if (subPane != null)
            {
                //log.debug("stateChanged: new pane ["+subPane.getTitle()+"]");
                // When the first the pane is added there is no notification via the listener so we nedd to do it here
                // when items are added and there is already items then the listener gets notified.
                if (currentPane != subPane)
                {
                    if (currentPane != null)
                    {
                        currentPane.showingPane(false);
                    }
                    ContextMgr.requestContext(subPane.getTask()); // XXX not sure if this need to be moved up into the if above
                    subPane.showingPane(true);
                    notifyListeners(NotificationType.Shown, subPane);
                }
             }
             currentPane = subPane;
       } else
       {
            ContextMgr.requestContext(null);
       }
    }

}
