/*
     * Copyright (C) 2007  The University of Kansas
     *
     * [INSERT KU-APPROVED LICENSE TEXT HERE]
     *
     */
/**
 * 
 */
package edu.ku.brc.specify.tasks.subpane.qb;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import edu.ku.brc.dbsupport.DBFieldInfo;
import edu.ku.brc.dbsupport.DBRelationshipInfo;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Oct 18, 2007
 *
 */
public class TableQRI extends ExpandableQRI
{
    protected DBRelationshipInfo relationship = null;
    protected boolean relChecked = false;

    public TableQRI(final TableTree tableTree)
    {
        super(tableTree);
        determineRel(); //probably not necessary
    }
        
    public void addField(final DBFieldInfo fieldInfo)
    {
        fields.add(new FieldQRI(this, fieldInfo));
    }
    
    public void addField(final FieldQRI fieldQRI)
    {
        fieldQRI.setTable(this);
        fields.add(fieldQRI);
    }
    
    public void addFieldClone(final FieldQRI fieldQRI) throws CloneNotSupportedException
    {
        FieldQRI newField = (FieldQRI)fieldQRI.clone();
        newField.setTable(this);
        fields.add(newField);
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.subpane.qb.BaseQRI#clone()
     */
    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        TableQRI result = (TableQRI)super.clone();
        result.fields = new Vector<FieldQRI>(fields.size());
        for (FieldQRI f : fields)
        {
            result.addFieldClone(f);
        }
        return result;
    }
    
    /**
     * @return relationship re
     */
    protected void determineRel()
    {
        Class<?> classObj = this.getTableTree().getTableInfo().getClassObj();
        List<DBRelationshipInfo> rels = new LinkedList<DBRelationshipInfo>();
        if (this.getTableTree().getParent() != null)
        {
            for (DBRelationshipInfo rel : this.getTableTree().getParent().getTableInfo()
                    .getRelationships())
            {
                if (rel.getDataClass().equals(classObj) && isRelevantRel(rel, classObj))
                {
                    rels.add(rel);
                }
            }
            if (rels.size() == 1) 
            { 
                relationship = rels.get(0); 
                return;
            }
            //XXX else ??? 
        }
        relationship = null;
    }

    /**
     * @param rel
     * @param classObj
     * @return false if rel represents a 'system' relationship.
     */
    protected static boolean isRelevantRel(final DBRelationshipInfo rel, final Class<?> classObj)
    {
        if (classObj.equals(edu.ku.brc.specify.datamodel.Agent.class))
        {
            return rel.getColName() == null ||
                (!rel.getColName().equalsIgnoreCase("modifiedbyagentid") &&
                 !rel.getColName().equalsIgnoreCase("createdbyagentid"));
        }
        return true;
    }

    /**
     * @return the relationship
     */
    public DBRelationshipInfo getRelationship()
    {
        if (relationship == null && !relChecked)
        {
            determineRel();
            relChecked = true;
        }
        return relationship;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.subpane.qb.BaseQRI#getTitle()
     */
    @Override
    public String getTitle()
    {
        if (relationship == null)
        {
            return super.getTitle();
        }
        return relationship.getTitle();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.tasks.subpane.qb.BaseQRI#setTableTree(edu.ku.brc.specify.tasks.subpane.qb.TableTree)
     */
    @Override
    public void setTableTree(TableTree tableTree)
    {
        super.setTableTree(tableTree);
        determineRel();
    }

    
    
}