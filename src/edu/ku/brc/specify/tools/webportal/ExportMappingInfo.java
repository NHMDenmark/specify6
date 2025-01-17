package edu.ku.brc.specify.tools.webportal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBRelationshipInfo;
import edu.ku.brc.af.core.db.DBTableChildIFace;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.specify.datamodel.Treeable;

/**
 * @author timo
 *
 */
public class ExportMappingInfo 
{
		final Integer mappingID;
		final String concept;
		final String conceptSchema;
		final Integer spTblId;
		final String spFldName;
		final String fldId;
		final Integer colIdx;
		final String colName;
		Boolean recordTyper = null;
		
		
		/**
		 * @param mappingID
		 * @param concept
		 * @param conceptSchema
		 * @param spTblId
		 * @param spFldName
		 * @param fldId
		 * @param colIdx
		 */
		public ExportMappingInfo(Integer mappingID, String concept, String conceptSchema,
				Integer spTblId, String spFldName, String fldId, Integer colIdx) 
		{
			super();
			this.mappingID = mappingID;
			this.concept = concept;
			this.conceptSchema = conceptSchema;
			this.spTblId = spTblId;
			this.spFldName = spFldName;
			this.fldId = fldId;
			this.colIdx = colIdx;
			if (this.concept == null)
			{
				this.colName = this.spTblId + "_" + spFldName;
			} else
			{
				this.colName = this.concept;
			}
		}

		/**
		 * @return
		 */
		public boolean isFullTextSearch()
		{
			//XXX need to add a way to configure this, and other stuff
			//cheap fix for cases people have complained about
			//This method needs to be duplicated by controller/widget/SearchCriterion.isFullTextFld() in the web portal
			String fldName = spFldName.toLowerCase();
			return fldName.indexOf("number") == -1 //StationFieldNumber, CatalogNumber, FieldNumber, ...
					&& fldName.indexOf("date") == -1 //StartDate, EndDate, ... 
					&& !fldName.startsWith("timestamp")
					&& !fldName.equalsIgnoreCase("guid");  
		}
		/**
		 * @return the mappingID
		 */
		public Integer getMappingID() 
		{
			return mappingID;
		}

		/**
		 * @return the concept
		 */
		public String getConcept() 
		{
			return concept;
		}

		/**
		 * @return the spTblId
		 */
		public Integer getSpTblId() 
		{
			return spTblId;
		}

		/**
		 * @return the spFldName
		 */
		public String getSpFldName() 
		{
			return spFldName;
		}

    /**
     *
     * @return
     */
    public String getFldTitle() {
		    DBFieldInfo info = getFldInfo();
		    if (info != null) {
		        return info.getTitle();
            } else {
		        return getSpFldName();
            }
        }

		/**
		 * @return the fldId
		 */
		public String getFldId() 
		{
			return fldId;
		}

		/**
		 * @return the colIdx
		 */
		public Integer getColIdx() 
		{
			return colIdx;
		}
		
		
		/**
		 * @return the conceptSchema
		 */
		public String getConceptSchema() {
			return conceptSchema;
		}

		/**
		 * @return the colName
		 */
		public String getColName() {
			return colName;
		}

		/**
		 * @return the info for the field
		 * 
		 * will return null for formatted,aggregated or treed fields
		 */
		public DBFieldInfo getFldInfo()
		{
			DBTableInfo tblInfo = getTblInfo();
			if (tblInfo !=  null)
			{
				return tblInfo.getFieldByName(getSpFldName());
			}
			return null;
		}
		
		/**
		 * @return relationship info for formatted/aggregated fields
		 * 
		 * returns null if getFldInfo() is not null
		 */
		public DBRelationshipInfo getRelInfo()
		{
			if (getFldInfo() == null) 
			{
				DBTableInfo tblInfo = getTblInfo();
				if (tblInfo !=  null)
				{
					tblInfo.getRelationshipByName(getSpFldName());
				}
			}	
			return null;
		}
	
		/**
		 * @return tree info for tree level fields
		 * 
		 * returns null in case of non-null getFldInfo() or getRelInfo() 
		 */
		public DBTreeLevelInfo getTreeInfo()
		{
			if (getFldInfo() == null && getRelInfo() == null && Treeable.class.isAssignableFrom(getTblInfo().getClassObj()))
			{
				String[] chunks = fldId.split("\\.");
				String levelStr = chunks[chunks.length-1];
				chunks = levelStr.split(" ");
				String rank = chunks[0];
				String fld = chunks.length == 1 ? "Name" : chunks[1];
				DBFieldInfo fldInfo = getTblInfo().getFieldByColumnName(fld);
				if (fldInfo != null)
				{
					return new DBTreeLevelInfo(rank, fldInfo);
				}	
			}
			return null;
		}
		
		public DBTableChildIFace getInfo()
		{
			DBTableChildIFace result = getFldInfo();
			if (result == null)
			{
				result = getRelInfo();
			} 
			if (result == null)
			{
				result = getTreeInfo();
			}
			return result;
		}
		
		/**
		 * @return
		 */
		public DBTableInfo getTblInfo()
		{
			return DBTableIdMgr.getInstance().getInfoById(getSpTblId());
		}
		
	    /**
	     * @return
	     */
	    public boolean isRecordTyper() {
	    	if (recordTyper == null) {
	    		recordTyper = false;
	    		DBFieldInfo fi = getFldInfo();
	    		if (fi != null) {
	    			Class<?> tblClass = fi.getTableInfo().getClassObj();
	    			try {
	    				Method m = tblClass.getMethod("getSpSystemTypeCodeFlds", (Class<?>[])null);
	    				String[] typerFlds = (String[])m.invoke(null, (Object[])null);
	    				for (String typerFld : typerFlds) {
	    					if (typerFld.equalsIgnoreCase(fi.getName())) {
	    						recordTyper = true;
	    						break;
	    					}
	    				}
	    			} catch (InvocationTargetException|IllegalAccessException|NoSuchMethodException mex) {
	    				//must not be a recordTyper
	    			}
	    		}
	    	}
	    	return recordTyper;
	    }

	}
