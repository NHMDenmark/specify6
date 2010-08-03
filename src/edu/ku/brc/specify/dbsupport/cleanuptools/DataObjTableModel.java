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
/**
 * 
 */
package edu.ku.brc.specify.dbsupport.cleanuptools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBInfoBase;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Created Date: Jun 18, 2010
 *
 */
/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Jun 29, 2010
 *
 */
public class DataObjTableModel extends DefaultTableModel
{
    protected Connection            conn;
    protected ArrayList<DBInfoBase> items;
    protected DBTableInfo           tableInfo;
    protected Vector<Object[]>      values     = null;
    protected boolean               isEditable = false;
    protected int[]                 mapInx     = null;
    
    protected String                colName;
    protected String                searchValue;
    protected int                   numColumns = 0;
    protected ArrayList<DBInfoBase> colDefItems  = new ArrayList<DBInfoBase>();
    protected ArrayList<Class<?>>   altClasses   = null;

    protected int                   hasDataCols = 0;
    protected ArrayList<Boolean>    sameValues  = null;
    protected ArrayList<Boolean>    hasDataList = null;
    protected ArrayList<DataObjTableModelRowInfo>    rowInfoList = new ArrayList<DataObjTableModelRowInfo>();
    
    protected HashMap<Integer, Integer> indexHash = new HashMap<Integer, Integer>();

    
    /**
     * @param conn
     * @param tableId
     * @param colName
     * @param value
     * @param isEditable
     */
    public DataObjTableModel(final Connection conn,
                             final int     tableId, 
                             final String  colName, 
                             final String  value,
                             final boolean isEditable)
    {
        super();
        
        this.conn        = conn;
        this.isEditable  = isEditable;
        this.colName     = colName;
        this.searchValue = value;
        this.tableInfo   = DBTableIdMgr.getInstance().getInfoById(tableId);
    }
    
    /**
     * @param conn
     * @param tableId
     * @param value
     * @param isEditable
     */
    public DataObjTableModel(final Connection conn,
                             final int     tableId, 
                             final String  value,
                             final boolean isEditable)
    {
        this(conn, tableId, null, value, isEditable);
        
        fillModels();
    }
    
    /**
     * @param conn
     * @param tableId
     * @param items
     * @param hasDataList
     * @param sameValues
     * @param mapInx
     * @param indexHash
     */
    public DataObjTableModel(final Connection conn,
                             final int tableId, 
                             final ArrayList<DBInfoBase>     items,
                             final ArrayList<Boolean>        hasDataList, 
                             final ArrayList<Boolean>        sameValues, 
                             final int[]                     mapInx, 
                             final HashMap<Integer, Integer> indexHash)
    {
        super();
        
        this.conn        = conn;
        this.items       = items;
        this.hasDataList = hasDataList;
        this.sameValues  = sameValues;
        this.mapInx      = mapInx;
        this.indexHash   = indexHash;
        this.isEditable  = true;
        
        this.tableInfo   = DBTableIdMgr.getInstance().getInfoById(tableId);
        
        this.values      = new Vector<Object[]>();
        this.values.add(new Object[items.size()]);
    }
    
    /**
     * @return the sql
     */
    protected String buildSQL()
    {
        StringBuffer sql = new StringBuffer("SELECT ");
        for (DBFieldInfo fi : tableInfo.getFields())
        {
            if (fi.getColumn().equals("Version")) continue;
            
            colDefItems.add(fi);
            if (colDefItems.size() > 1) sql.append(',');
            sql.append(fi.getColumn());
        }
        numColumns = colDefItems.size();
        
        sql.append(" FROM %s WHERE %s LIKE ?");
        String sqlStr = String.format(sql.toString(), tableInfo.getName(), colName, searchValue + '%');
        
        return sqlStr;
    }
    
    /**
     * The Data members must be set to call this:
     *     numColumns
     *     itemsList
     * 
     */
    protected void fillModels()
    {
        final String sqlStr = buildSQL();
        System.out.println(sqlStr);
        
        try
        {
            values = new Vector<Object[]>();
            
            PreparedStatement pStmt = conn.prepareStatement(sqlStr);
            pStmt.setString(1, searchValue);
            //System.out.println(sqlStr+" ["+searchValue+"]");
            
            ResultSet rs = pStmt.executeQuery();
            while (rs.next())
            {
                Object[] row = new Object[numColumns];
                for (int i=0;i<numColumns;i++)
                {
                    row[i] = rs.getObject(i+1);
                }
                rowInfoList.add(new DataObjTableModelRowInfo(rs.getInt(1), false, false));
                values.add(row);
            }
            rs.close();
            pStmt.close();
            
        } catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        
        addAdditionalRows(colDefItems, rowInfoList);
        
        sameValues  = new ArrayList<Boolean>(numColumns);
        hasDataList = new ArrayList<Boolean>(numColumns);
        for (int i=0;i<numColumns;i++)
        {
            sameValues.add(true);
            hasDataList.add(false);
        }
        
        for (Object[] col : values)
        {
            for (int i=0;i<numColumns;i++)
            {
                Object  data    = col[i];
                boolean hasData = data != null;
                
                if (hasData && !hasDataList.get(i))
                {
                    hasDataList.set(i, true);
                    hasDataCols++;
                }
            }
        }
        
        adjustHasDataColumns();
        
        mapInx = new int[hasDataCols];
        int colInx = 0;
        System.out.println("-------------Has Data----------------------");
        for (int i=0;i<numColumns;i++)
        {
            if (hasDataList.get(i))
            {
                //System.out.println(itemsList.get(i).getTitle());
                mapInx[colInx] = i;
                indexHash.put(i, colInx);
                //System.out.print("indexHash: "+i +" -> "+colInx);
                //System.out.println("  mapInx:    "+colInx +" -> "+i);
                colInx++;
            }
        }        
        
        for (int i=0;i<mapInx.length;i++)
        {
            colInx = mapInx[i];
            
            if (hasDataList.get(colInx))
            {
                Object data = null;
                for (Object[] col : values)
                {
                    Object newData = col[colInx];
                    
                    if (data == null)
                    {
                        if (newData != null)
                        {
                            data = newData;
                        }
                        continue;
                    }
                   
                    if (newData != null && !data.equals(newData))
                    {
                        sameValues.set(colInx, false);
                        break;
                    }
                }
            }
        }
        
        /*
        System.out.println("-----------Same------------------------");
        for (int i=0;i<mapInx.length;i++)
        {
            colInx = mapInx[i];
            if (sameValues.get(colInx))
            {
                System.out.println(colInx + " " + itemsList.get(colInx).getTitle());
            }
        }*/
        
        items = new ArrayList<DBInfoBase>(colDefItems);
    }
    
    /**
     * 
     */
    protected void addAdditionalRows(@SuppressWarnings("unused") final ArrayList<DBInfoBase> colDefItems,
                                     @SuppressWarnings("unused") final ArrayList<DataObjTableModelRowInfo> rowInfoList)
    {
        
    }
    
    /**
     * 
     */
    protected void adjustHasDataColumns()
    {
        
    }
    
    /**
     * @return the rowInfoList
     */
    public ArrayList<DataObjTableModelRowInfo> getRowInfoList()
    {
        return rowInfoList;
    }

    /**
     * @param column
     * @return
     */
    public boolean isSame(final int column)
    {
        return sameValues.get(mapInx[column]);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.DefaultTableModel#getColumnCount()
     */
    @Override
    public int getColumnCount()
    {
        return mapInx != null ? mapInx.length : 0;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.DefaultTableModel#getColumnName(int)
     */
    @Override
    public String getColumnName(int column)
    {
        if (column == 0) 
        {
            return !isEditable ? "Is Included" : ""; // I18N
        }
        
        return mapInx != null ? items.get(mapInx[column]).getTitle() : "";
    }

    /* (non-Javadoc)
     * @see javax.swing.table.DefaultTableModel#getRowCount()
     */
    @Override
    public int getRowCount()
    {
        return values != null ? values.size() : 0;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.DefaultTableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(int row, int column)
    {
        if (column == 0) 
        {
            if (!isEditable)
            {
                return rowInfoList.size() > 0 ? rowInfoList.get(row).isIncluded() : false;
            }
            return "";
        }

        //System.out.println("----------------");
        Object[] col = values != null ? values.get(row) : null;
        if (col != null)
        {
            //System.out.println("column "+column);
            //System.out.println("mapInx[column] "+mapInx[column]);
            //System.out.println("col len "+col.length);
            return col[mapInx[column]];
        }
        return null;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
     */
    @Override
    public Class<?> getColumnClass(int column)
    {
        if (column == 0)
        {
            return !isEditable ? Boolean.class : String.class;
        }
        
        int mappedInx = mapInx[column];
        
        if (altClasses == null)
        {
            DBInfoBase base = items.get(mappedInx);
            if (base instanceof DBFieldInfo)
            {
                return ((DBFieldInfo)base).getDataClass();
            }
        } else
        {
            return altClasses.get(mappedInx);
        }
        return String.class;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
     */
    @Override
    public boolean isCellEditable(int row, int column)
    {
        if (column == 0)
        {
            return !isEditable; 
        }

        return isEditable;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.DefaultTableModel#setValueAt(java.lang.Object, int, int)
     */
    @Override
    public void setValueAt(Object aValue, int row, int column)
    {
        if (column == 0)
        {
            if (!isEditable && aValue instanceof Boolean)
            {
                rowInfoList.get(row).setIncluded((Boolean)aValue);
            }    
            return;
        }
        
        Object[] col = values != null ? values.get(row) : null;
        if (col != null)
        {
            col[mapInx[column]] = aValue;
            fireTableCellUpdated(row, column);
        }
    }

    /**
     * @return the items
     */
    public ArrayList<DBInfoBase> getItems()
    {
        return items;
    }

    /**
     * @param items the items to set
     */
    public void setItems(ArrayList<DBInfoBase> items)
    {
        this.items = items;
    }

    /**
     * @return the mapInx
     */
    public int[] getMapInx()
    {
        return mapInx;
    }

    /**
     * @param mapInx the mapInx to set
     */
    public void setMapInx(int[] mapInx)
    {
        this.mapInx = mapInx;
    }

    /**
     * @return the sameValues
     */
    public ArrayList<Boolean> getSameValues()
    {
        return sameValues;
    }

    /**
     * @param sameValues the sameValues to set
     */
    public void setSameValues(ArrayList<Boolean> sameValues)
    {
        this.sameValues = sameValues;
    }

    /**
     * @return the hasDataList
     */
    public ArrayList<Boolean> getHasDataList()
    {
        return hasDataList;
    }

    /**
     * @param hasDataList the hasDataList to set
     */
    public void setHasDataList(ArrayList<Boolean> hasDataList)
    {
        this.hasDataList = hasDataList;
    }

    /**
     * @return the indexHash
     */
    public HashMap<Integer, Integer> getIndexHash()
    {
        return indexHash;
    }

    /**
     * @param indexHash the indexHash to set
     */
    public void setIndexHash(HashMap<Integer, Integer> indexHash)
    {
        this.indexHash = indexHash;
    }
}
