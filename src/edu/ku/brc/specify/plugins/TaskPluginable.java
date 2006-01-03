/* Filename:    $RCSfile: TaskPluginable.java,v $
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

package edu.ku.brc.specify.plugins;

import java.util.List;
/**
 * An interface for describing how a plugin can be registered into the UI and begin to provide service.
 * 
 * External plugins should make sure the call <i>initialize</i> after they install themselves, or they can call
 * PluginMgr.initilize();
 * 
 * @author rods
 *
 */
public interface TaskPluginable
{
    /**
     * Initializes the task. The Taskable is responsible for making sure this method 
     * can be called mulitple times with no ill effects.
     *
     */
    public void initialize();
    

    /**
     * Returns the name of the task (NOT Localized)
     * @return Returns the name of the task (NOT Localized)
     */
    public String getName();
    
    
    /**
     * Returns the toolbar items (usually only one item)
     * @return Returns the toolbar items (usually only one item)
     */
    public List<ToolBarItemDesc> getToolBarItems();
    
    /**
     * Returns the menu item to be registered
     * @return Returns the menu item to be registered
     */
    public List<MenuItemDesc>    getMenuItems();
    
}
