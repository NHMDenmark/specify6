/* Filename:    $RCSfile: ActionChangedListener.java,v $
 * Author:      $Author: rods $
 * Revision:    $Revision: 1.1 $
 * Date:        $Date: 2005/10/19 19:59:54 $
 *
 * This library is free software; you can redistribute it and/or
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
package edu.ku.brc.specify.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;

/**
 * 
 * @author Rod Spears
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ActionChangedListener implements PropertyChangeListener
{
   private Vector<JComponent> items = new Vector<JComponent>(); 
    
    public ActionChangedListener(JComponent aUIComp)
    {
        super();
        add(aUIComp);
    }
    
    public void add(JComponent aUIComp)
    {
        items.addElement(aUIComp);
    }
    
    public void remove(JComponent aUIComp)
    {
        items.removeElement(aUIComp);
    }
    
    public void propertyChange(PropertyChangeEvent e)
    {
        for (JComponent comp : items)
        {
            String propertyName = e.getPropertyName();
            if (comp instanceof AbstractButton && e.getPropertyName().equals(Action.NAME))
            {
                String text = (String) e.getNewValue();
                ((AbstractButton)comp).setText(text);
                
            } else if (propertyName.equals("enabled"))
            {
                Boolean enabledState = (Boolean) e.getNewValue();
                comp.setEnabled(enabledState.booleanValue());
            }
        }
    }
}