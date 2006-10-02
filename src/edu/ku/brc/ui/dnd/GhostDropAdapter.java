package edu.ku.brc.ui.dnd;

import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * (Adpated from Romain Guy's Glass Pane Drag Photo Demo)
 *
 * @code_status Beta
 * 
 * @author rods
 * @author Romain Guy
 * @author Sebastien Petrucci <sebastien_petrucci@yahoo.fr>*
 *
 */
public class GhostDropAdapter extends MouseAdapter 
{
    protected GhostGlassPane glassPane;

    protected String action;

    private List<GhostDropListener> listeners;

    public GhostDropAdapter(GhostGlassPane glassPane, String action) 
    {
        this.glassPane = glassPane;
        this.action = action;
        this.listeners = new ArrayList<GhostDropListener>();
    }

    public void addGhostDropListener(GhostDropListener listener) 
    {
        if (listener != null)
            listeners.add(listener);
    }

    public void removeGhostDropListener(GhostDropListener listener) 
    {
        if (listener != null)
            listeners.remove(listener);
    }

    protected void fireGhostDropEvent(GhostDropEvent evt) 
    {
        Iterator<GhostDropListener> it = listeners.iterator();
        while (it.hasNext()) 
        {
            it.next().ghostDropped(evt);
        }
    }
    
    protected boolean hasListeners()
    {
        return listeners.size() > 0;
    }
}
