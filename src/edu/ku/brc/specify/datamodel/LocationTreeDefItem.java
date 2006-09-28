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
package edu.ku.brc.specify.datamodel;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class LocationTreeDefItem extends DataModelObjBase implements Serializable, TreeDefItemIface<Location,LocationTreeDef,LocationTreeDefItem>
{

	// Fields    

	protected Long   				locationTreeDefItemId;
	protected String				name;
	protected String				remarks;
	protected Integer				rankId;
	protected Boolean				isEnforced;
	protected Boolean				isInFullName;
	protected LocationTreeDef			treeDef;
	protected LocationTreeDefItem		parent;
	protected Set<Location>			treeEntries;
	protected Set<LocationTreeDefItem>	children;

	// Constructors

	/** default constructor */
	public LocationTreeDefItem()
	{
		// do nothing
	}

	/** constructor with id */
	public LocationTreeDefItem(Long locationTreeDefItemId)
	{
		this.locationTreeDefItemId = locationTreeDefItemId;
	}

	// Initializer
	public void initialize()
	{
		locationTreeDefItemId = null;
		name = null;
		remarks = null;
		rankId = null;
		isEnforced = null;
		isInFullName = null;
		treeDef = null;
		treeEntries = new HashSet<Location>();
		parent = null;
		children = new HashSet<LocationTreeDefItem>();
	}

	// End Initializer

	// Property accessors

	/**
	 * 
	 */
	public Long getLocationTreeDefItemId()
	{
		return this.locationTreeDefItemId;
	}

    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    public Long getId()
    {
        return this.locationTreeDefItemId;
    }

	public void setLocationTreeDefItemId(Long locationTreeDefItemId)
	{
		this.locationTreeDefItemId = locationTreeDefItemId;
	}

	/**
	 * 
	 */
	public String getName()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * 
	 */
	public String getRemarks()
	{
		return this.remarks;
	}

	public void setRemarks(String remarks)
	{
		this.remarks = remarks;
	}

	/**
	 * 
	 */
	public Integer getRankId()
	{
		return this.rankId;
	}

	public void setRankId(Integer rankId)
	{
		this.rankId = rankId;
	}

	/**
	 * 
	 */
	public Boolean getIsEnforced()
	{
		return this.isEnforced;
	}

	public void setIsEnforced(Boolean isEnforced)
	{
		this.isEnforced = isEnforced;
	}

	public Boolean getIsInFullName()
	{
		return isInFullName;
	}

	public void setIsInFullName(Boolean isInFullName)
	{
		this.isInFullName = isInFullName;
	}

	/**
	 * 
	 */
	public LocationTreeDef getTreeDef()
	{
		return this.treeDef;
	}

	public void setTreeDef(LocationTreeDef treeDef)
	{
		this.treeDef = treeDef;
	}

	/**
	 * 
	 */
	public LocationTreeDefItem getParent()
	{
		return this.parent;
	}

	public void setParent(LocationTreeDefItem parent)
	{
		this.parent = parent;
	}

	/**
	 * 
	 */
	public Set<Location> getTreeEntries()
	{
		return this.treeEntries;
	}

	public void setTreeEntries(Set<Location> treeEntries)
	{
		this.treeEntries = treeEntries;
	}

	/**
	 * 
	 */
	public Set<LocationTreeDefItem> getChildren()
	{
		return this.children;
	}

	public void setChildren(Set<LocationTreeDefItem> children)
	{
		this.children = children;
	}

	// Code added to implement TreeDefinitionItemIface

	public Long getTreeDefItemId()
	{
		return getLocationTreeDefItemId();
	}

	public void setTreeDefItemId(Long id)
	{
		setLocationTreeDefItemId(id);
	}

	public void setChild(LocationTreeDefItem child)
	{
		if( child==null )
		{
			children = new HashSet<LocationTreeDefItem>();
			return;
		}

		children = new HashSet<LocationTreeDefItem>();
		children.add(child);
	}
	
	public LocationTreeDefItem getChild()
	{
		if(children.isEmpty())
		{
			return null;
		}
		return children.iterator().next();
	}

	public void addTreeEntry(Location entry)
	{
		treeEntries.add(entry);
		entry.setDefinitionItem(this);
	}

	public void removeTreeEntry(Location entry)
	{
		treeEntries.remove(entry);
		entry.setDefinitionItem(null);
	}

	public void removeChild(LocationTreeDefItem child)
	{
		children.remove(child);
		child.setParent(null);
	}
	
	public boolean canBeDeleted()
	{
		if(treeEntries.isEmpty())
		{
			return true;
		}
		return false;
	}
}
