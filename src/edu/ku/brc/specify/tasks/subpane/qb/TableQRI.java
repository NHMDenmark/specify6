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

import org.apache.commons.lang.StringUtils;

import edu.ku.brc.dbsupport.DBTableInfo;
import edu.ku.brc.ui.UIHelper;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Oct 18, 2007
 *
 */
public class TableQRI extends BaseQRI
{
    protected DBTableInfo ti;
    
    public TableQRI(final BaseQRI parent, final TableTree tableTree)
    {
        super(parent, tableTree);
        this.ti  = tableTree.getTableInfo();
        iconName = ti.getClassObj().getSimpleName();
        title    = ti.getTitle();
        if (StringUtils.isEmpty(title))
        {
            title    = UIHelper.makeNamePretty(iconName);
        }
    }
    
    public DBTableInfo getTableInfo()
    {
        return ti;
    }
}