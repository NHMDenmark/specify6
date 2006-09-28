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
public class TaxonTreeDefItem extends DataModelObjBase implements Serializable, TreeDefItemIface<Taxon,TaxonTreeDef,TaxonTreeDefItem>
{

	// Fields    

	protected Long				    taxonTreeDefItemId;
	protected String				name;
	protected String				remarks;
	protected Integer				rankId;
	protected Boolean				isEnforced;
	protected Boolean				isInFullName;
	protected TaxonTreeDef			treeDef;
	protected TaxonTreeDefItem		parent;
	protected Set<Taxon>			treeEntries;
	protected Set<TaxonTreeDefItem>	children;

	// Constructors

	/** default constructor */
	public TaxonTreeDefItem()
	{
		// do nothing
	}

	/** constructor with id */
	public TaxonTreeDefItem(Long taxonTreeDefItemId)
	{
		this.taxonTreeDefItemId = taxonTreeDefItemId;
	}

	// Initializer
	public void initialize()
	{
		taxonTreeDefItemId = null;
		name = null;
		remarks = null;
		rankId = null;
		isEnforced = null;
		isInFullName = null;
		treeDef = null;
		treeEntries = new HashSet<Taxon>();
		parent = null;
		children = new HashSet<TaxonTreeDefItem>();
	}

	// End Initializer

	// Property accessors

	/**
	 * 
	 */
	public Long getTaxonTreeDefItemId()
	{
		return this.taxonTreeDefItemId;
	}

    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    public Long getId()
    {
        return this.taxonTreeDefItemId;
    }

	public void setTaxonTreeDefItemId(Long taxonTreeDefItemId)
	{
		this.taxonTreeDefItemId = taxonTreeDefItemId;
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
	public TaxonTreeDef getTreeDef()
	{
		return this.treeDef;
	}

	public void setTreeDef(TaxonTreeDef treeDef)
	{
		this.treeDef = treeDef;
	}

	/**
	 * 
	 */
	public TaxonTreeDefItem getParent()
	{
		return this.parent;
	}

	public void setParent(TaxonTreeDefItem parent)
	{
		this.parent = parent;
	}

	/**
	 * 
	 */
	public Set<Taxon> getTreeEntries()
	{
		return this.treeEntries;
	}

	public void setTreeEntries(Set<Taxon> treeEntries)
	{
		this.treeEntries = treeEntries;
	}

	/**
	 * 
	 */
	public Set<TaxonTreeDefItem> getChildren()
	{
		return this.children;
	}

	public void setChildren(Set<TaxonTreeDefItem> children)
	{
		this.children = children;
	}

	// Code added to implement TreeDefinitionItemIface

	public Long getTreeDefItemId()
	{
		return getTaxonTreeDefItemId();
	}

	public void setTreeDefItemId(Long id)
	{
		setTaxonTreeDefItemId(id);
	}

	public void setChild(TaxonTreeDefItem child)
	{
		if( child==null )
		{
			children = new HashSet<TaxonTreeDefItem>();
			return;
		}

		children = new HashSet<TaxonTreeDefItem>();
		children.add(child);
	}
	
	public TaxonTreeDefItem getChild()
	{
		if(children.isEmpty())
		{
			return null;
		}
		return children.iterator().next();
	}

	public void addTreeEntry(Taxon entry)
	{
		treeEntries.add(entry);
		entry.setDefinitionItem(this);
	}

	public void removeTreeEntry(Taxon entry)
	{
		treeEntries.remove(entry);
		entry.setDefinitionItem(null);
	}

	public void removeChild(TaxonTreeDefItem child)
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
