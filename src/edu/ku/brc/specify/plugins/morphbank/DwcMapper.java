/**
 * 
 */
package edu.ku.brc.specify.plugins.morphbank;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import edu.ku.brc.af.core.db.DBFieldInfo;
import edu.ku.brc.af.core.db.DBTableIdMgr;
import edu.ku.brc.af.core.db.DBTableInfo;
import edu.ku.brc.af.ui.forms.formatters.DataObjFieldFormatMgr;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterIFace;
import edu.ku.brc.af.ui.forms.formatters.UIFieldFormatterMgr;
import edu.ku.brc.dbsupport.DBConnection;
import edu.ku.brc.dbsupport.DataProviderFactory;
import edu.ku.brc.dbsupport.DataProviderSessionIFace;
import edu.ku.brc.specify.conversion.BasicSQLUtils;
import edu.ku.brc.specify.datamodel.CollectionObject;
import edu.ku.brc.specify.datamodel.DataModelObjBase;
import edu.ku.brc.specify.datamodel.Determination;
import edu.ku.brc.specify.datamodel.TreeDefIface;
import edu.ku.brc.specify.datamodel.TreeDefItemIface;
import edu.ku.brc.specify.datamodel.Treeable;
import edu.ku.brc.specify.tasks.subpane.qb.DateAccessorQRI;
import edu.ku.brc.specify.tools.export.ConceptMapUtils;
import edu.ku.brc.specify.tools.export.ExportPanel;
import edu.ku.brc.specify.tools.export.MappedFieldInfo;
import edu.ku.brc.ui.UIRegistry;

/**
 * @author timo
 *
 * @code_status Alpha
 *
 * Apr 7, 2010
 */
public class DwcMapper
{
	final Integer mappingId; //SpExportSchemaMappingID - key for spexportschemamappingid.
	final String mappingName;
	final String schemaName;
	final Integer mappingContextTableId;
	final String schemaURL;
	final Boolean getAllManies;
	
	private static boolean debugging = false;
	
	final Vector<MappingInfo> concepts = new Vector<MappingInfo>();
	
	public static Connection connection; //for testing
	
	/**
	 * @param mappingId
	 */
	public DwcMapper(Integer mappingId)
	{
		this(mappingId, false);
	}
	
	/**
	 * @return
	 */
	protected Connection getConnection() {
		if (connection == null) {
			connection = DBConnection.getInstance().getConnection();
		}
		return connection;
	}
	
	public DwcMapper(Integer mappingId, boolean getAllManies)
	{
		this.mappingId = mappingId;
		this.getAllManies = getAllManies;
		Vector<Object[]> rec = BasicSQLUtils.query(getConnection(), getMappingQuery(mappingId));
		mappingName = (String )rec.get(0)[0];
		schemaName = (String )rec.get(0)[1];
		mappingContextTableId = (Integer )rec.get(0)[2];
		schemaURL = (String )rec.get(0)[3];
		fillConcepts();
		Collections.sort(concepts);
//		for (MappingInfo mi : concepts)
//		{
//			System.out.println(mi.getMapping() + "  " + mi.getName());
//		}
	}
	
	/**
	 * 
	 */
	public DwcMapper()
	{
		mappingId = null;
		getAllManies = false;
		mappingName = UIRegistry.getResourceString("DwcMapper.Default");
		schemaName = null;
		mappingContextTableId = CollectionObject.getClassTableId();
		schemaURL = null;
		fillDefaultConcepts();
		Collections.sort(concepts);
	}
	
	/**
	 * @param mappingId
	 * @return query to extract mapping from 'sp' tables for given mapping key
	 */
	protected String getMappingQuery(Integer mappingId)
	{
		return "select esm.MappingName, es.SchemaName, q.ContextTableId, es.Description from spexportschemamapping esm inner join "
			+ "spexportschemaitemmapping esim on esim.SpExportSchemaMappingID = esm.SpExportSchemaMappingID "
			+ "left join spexportschemaitem esi on esi.SpExportSchemaItemID = esim.ExportSchemaItemID inner join " 
			+ "spexportschema es on es.SpExportSchemaID = esi.SpExportSchemaID inner join spqueryfield qf on "
			+ "qf.SpQueryFieldID = esim.SpQueryFieldID inner join spquery q on q.SpQueryID = qf.SpQueryID where "
			+ "qf.IsDisplay and esm.SpExportSchemaMappingID = " + mappingId;
	}
	
	/**
	 * 
	 */
	protected void fillConcepts()
	{
		Vector<Object[]> cpts = BasicSQLUtils.query(connection, getConceptQuery());
		concepts.clear();
		for (Object[] concept : cpts)
		{
			if (concept[1] != null) //DwC concept mapping is required 
			{
				concepts.add(new MappingInfo((String )concept[0], (String )concept[4], (String )concept[1], (String )concept[2], 
					mappingContextTableId, (Boolean )concept[3]));
			}
		}	
	}
	
	/**
	 * 
	 */
	protected void fillDefaultConcepts()
	{
		concepts.clear();
		for (Map.Entry<String, Vector<MappedFieldInfo>> me : ConceptMapUtils.getDefaultDarwinCoreMappings().entrySet())
		{
			concepts.add(new MappingInfo(me.getKey(), me.getValue().get(0)));
		}
	}
	
	/**
	 * @param mappingId
	 * @return
	 */
	protected String getMappingName(Integer mappingId)
	{
		return BasicSQLUtils.querySingleObj(connection, 
				"select MappingName from spexportschemamapping where "
				+ " SpExportSchemaMappingID = " + mappingId);
	}
	
	/**
	 * @return
	 */
	protected String getConceptQuery()
	{
		return "select esi.FieldName, esi.DataType, qf.StringId, qf.IsRelFld, es.description from spexportschemaitemmapping esim inner join spexportschemaitem esi on "
			+ "esi.SpExportSchemaItemID = esim.ExportSchemaItemID inner join spexportschema es on es.spexportschemaid = esi.spexportschemaid "
			+ "inner join spqueryfield qf on qf.SpQueryFieldID = esim.SpQueryFieldID where qf.IsDisplay and esim.SpExportSchemaMappingID = "
			+ mappingId;
	}
	
	/**
	 * @param spec
	 * @throws Exception
	 */
	public void setDarwinCoreConcepts(DarwinCoreSpecimen spec) throws Exception
	{
		spec.clearConcepts();
		for (MappingInfo mi : concepts)
		{
			spec.add(mi.getTerm(), null);
		}
	}
	
	/**
	 * @param collectionObjectId
	 * @return query to retrieve darwin core record from the cache.
	 */
	protected String getValuesQuery(Integer collectionObjectId)
	{
		//return "select * from " + ExportToMySQLDB.fixTblNameForMySQL(mappingName) + " where " + ExportToMySQLDB.fixTblNameForMySQL(mappingName) + "id = " + collectionObjectId; 
		//XXX ExportPanel.getCacheTableName assumes current collection. Is it possible for collectionObjectId to identify a record in another collection???
		//return "select * from " + ExportPanel.getCacheTableName(mappingName) + " where " + ExportPanel.getCacheTableName(mappingName) + "id = " + collectionObjectId; 
		
		List<String> selects = new ArrayList<String>();
		selects.add(ExportPanel.getCacheTableName(mappingName) + "id"); 
		for (MappingInfo mi : concepts) {
			if (java.util.Date.class.equals(mi.getDataType())) {
				selects.add("date_format(`" + mi.getName() + "`,'%Y-%m-%d') `" + mi.getName() + "`");
			} else {
				selects.add("`" + mi.getName() + "`");
			}
		}
		String flds = selects.toString();
		flds = flds.substring(1, flds.length() - 1);
		return "select " + flds + " from " + ExportPanel.getCacheTableName(mappingName) + " where " + ExportPanel.getCacheTableName(mappingName) + "id = " + collectionObjectId; 
	}
	
	
	
	/**
	 * @param spec
	 * @throws Exception
	 * 
	 * Get the darwin core values for the specimen.
	 * 
	 * Currently this just gets the values from the cache created by the export tool. Will need
	 * to get live values if the cache is not built or the cache record is out of date or the cache does not 
	 * contain the specimen yet. May turn out to be unnecessary to use the cache.
	 * Current idea for use when cache won't work is to get the ExportMapper to fill in the SpQuery.sql field
	 * with something that can be run without having to setup a querybuilder -- though perhaps something
	 * like what is done with the qb when reports run will work...
	 * 
	 * Also, given a CollectionObject object it should be possible to use MappingInfo.mapping to follow relationships from the CO to the mapped fields.
	 * 
	 */
	
	public void setDarwinCoreValues(DarwinCoreSpecimen spec) throws Exception
	{
		if (/*true*/spec.hasDataModelObject())
		{
			setDarwinCoreValuesForObj(spec);
		}
		else
		{
			setDarwinCoreValuesForId(spec);
		}
	}
	
	/**
	 * @param spec
	 * @throws Exception
	 */
	protected void setDarwinCoreValuesForId(DarwinCoreSpecimen spec) throws Exception
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = getConnection().createStatement();
			rs = stmt.executeQuery(getValuesQuery(spec.getCollectionObjectId()));
			if (!rs.next())
			{
				throw new MissingRecordException("no record for " + spec.getCollectionObjectId() + " in " + mappingName,
						spec.getCollectionObjectId(), mappingName);
			}
			ResultSetMetaData metaData = rs.getMetaData();
			for (int c = 2; c <= metaData.getColumnCount(); c++)
			{
				String colName = metaData.getColumnLabel(c);
				spec.set(colName, rs.getObject(colName));
			}
		}
		finally
		{
			if (rs != null) rs.close();
			if (stmt != null) stmt.close();
		}
	}
	
	/**
	 * @param spec
	 * @throws Exception
	 */
	protected void setDarwinCoreValuesForObj(DarwinCoreSpecimen spec) throws Exception
	{
		//throw new Exception("No code is present to do this thing.");
		
		//Using hibernate objects and reflection ...
		for (MappingInfo mi : concepts)
		{
			if (debugging) {
				System.out.println("DwcMapper.setDarwinCoreValuesForObj -- setting " + mi.getName() + " : " + mi.getMappedFieldName() + ", isFormatted: " + mi.isFormatted());
				if (mi.getName().equals("dateIdentified")) {
					System.out.println("stop");
				}
			}
			spec.set(mi.getTerm(), getMappedValue(mi, spec.getCollectionObject()));
		}
		
		//But maybe it is easier to construct a query or to create and save a query for this purpose into SpQuery.SqlStr (even though it will have to be hql for now)
		//hibernate strategy is beginning to suck. And what about lazy-loading and session attachment, and the chance for messing things up in the form system's session...???
	}
	
	/**
	 * @param mi
	 * @param obj
	 * @return
	 */
	protected Object getMappedValue(MappingInfo mi, DataModelObjBase obj) throws Exception
	{
		String[] mapSegments = mi.getMapping().split(",");
		DataModelObjBase currentObject = obj;
		DataProviderSessionIFace session = DataProviderFactory.getInstance().createSession();
		try
		{
			session.attach(currentObject);
			
			if (mapSegments.length == 1)
			{
				return getValueFromObject(currentObject, mapSegments[0], mi.isFormatted(), mi.isTreeRank(), session);
			}
			
			return getMappedValue2(mi, mapSegments, 1, currentObject, getAllManies, session);
			/*for (int s = 1; s < mapSegments.length; s++)
			{
				//System.out.println(mapSegments[s]);

				if (currentObject != null
						&& (s < mapSegments.length - 1 || !mi.isFormatted()))
				{
					currentObject = getRelatedObject(currentObject,
							mapSegments[s]);
				}
				if (currentObject == null)
				{
					return null;
				}
				
				session.attach(currentObject); //shouldn't have to do this explicitly???
				
				//System.out.println("   "
				//		+ currentObject.getClass().getSimpleName());

				if (s == mapSegments.length - 1)
				{
					return getValueFromObject(currentObject, mapSegments[s], mi
							.isFormatted(), mi.isTreeRank(), session);
				}
			}*/
		} finally
		{
			session.close();
		}
		//return null;
	}
	
	
	protected Object getMappedValue2(MappingInfo mi, String[] mapSegments, int segIdx, DataModelObjBase currentObj, boolean getManies,
			DataProviderSessionIFace session) throws Exception
	{
		DataModelObjBase currentObject = currentObj;
			//System.out.println(mapSegments[s]);
		List<DataModelObjBase> currentObjects = new ArrayList<DataModelObjBase>();
		List<Object> results = new ArrayList<Object>();
		boolean returnFirstResult = true;
		if (currentObject != null
				&& (segIdx < mapSegments.length - 1 || !mi.isFormatted()))
		{
			if (getManies) 
			{
				Object objs = getRelatedObjects(currentObject, mapSegments[segIdx]);
				if (objs != null)
				{
					if (!Collection.class.isAssignableFrom(objs.getClass()))
					{
						currentObjects.add((DataModelObjBase )objs);
					} else
					{
						returnFirstResult = false;
						currentObjects.addAll((Collection<? extends DataModelObjBase>)objs);
					}
				}
			} else
			{
				currentObject = getRelatedObject(currentObject,
					mapSegments[segIdx]);
				if (currentObject != null)
				{
					currentObjects.add(currentObject);
				}
			}
			if (currentObjects.size() == 0)
			{
				return null;
			}
			
			for (DataModelObjBase obj : currentObjects)
			{
				session.attach(obj); //shouldn't have to do this explicitly???
				
				//System.out.println("   "
				//		+ currentObject.getClass().getSimpleName());

				if (segIdx == mapSegments.length - 1)
				{
					results.add(getValueFromObject(obj, mapSegments[segIdx], mi
						.isFormatted(), mi.isTreeRank(), session));
				} else
				{
					results.add(getMappedValue2(mi, mapSegments, segIdx+1, obj, false /*NO manies off manies!*/, session));
				}
			}
		} else if (mi.isFormatted())
		{
			return getValueFromObject(currentObj, mapSegments[segIdx], mi
					.isFormatted(), mi.isTreeRank(), session);
		}
		if (results.size() == 0)
		{
			return null;
		} else if (results.size() == 1 && returnFirstResult)
		{
			return results.get(0); 
		} else 
		{
			return results;
		}
	}
	
	/**
	 * @param object
	 * @param mapping
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected DataModelObjBase getRelatedObject(DataModelObjBase object, String mapping) throws Exception
	{
		Object objs = getRelatedObjects(object, mapping);
		
		if (objs == null)
		{
			return null;
		}
		
		if (!Collection.class.isAssignableFrom(objs.getClass()))
		{
			return (DataModelObjBase )objs;
		}
		if (((Collection<?> )objs).size() == 0)
		{
			return null;
		}
		if (((Collection<?> )objs).size() == 1)
		{
			return (DataModelObjBase )((Collection<?> )objs).iterator().next();
		}
		return selectRelatedObject(object, (Collection<DataModelObjBase> )objs);
	}
	
	/**
	 * @param object
	 * @param mapping
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	protected Object getRelatedObjects(DataModelObjBase object, String mapping) throws Exception
	{
		String[] mapInfo = mapping.split("-");
		int tableId = Integer.parseInt(mapInfo[0].split("\\.")[0]);
		String relationshipName = mapInfo.length > 1 ? mapInfo[1].split("\\.")[0] : null;
		Class<? extends DataModelObjBase> relatedClass = (Class<? extends DataModelObjBase> )DBTableIdMgr.getInstance().getInfoById(tableId).getClassObj();
		String methName = relationshipName == null ?  "get" + relatedClass.getSimpleName() 
				: "get" + relationshipName.substring(0, 1).toUpperCase().concat(relationshipName.substring(1));
		Method meth = object.getClass().getMethod(methName);
		return meth.invoke(object);
	}

	/**
	 * @param parent
	 * @param getter
	 * @return appropriate related object
	 * 
	 * This is used for one-to-many relationships where getter returns a set of related objects.
	 * In most cases it should be safe to assume a criterion for picking based on the related class type.
	 * 
	 * Currently criteria are Determination - isCurrent, Collector - isPrimary, others just the first...
	 * 
	 * Hopefully, in the vast majority of cases, one-to-many's will be handled by aggregations or formatters and this method will not be needed.
	 */
	protected DataModelObjBase selectRelatedObject(DataModelObjBase parent, Collection<DataModelObjBase> objects) throws Exception
	{
		if (objects.size() > 0)
		{
			Iterator<? extends DataModelObjBase> iter = objects.iterator();
			while(iter.hasNext())
			{
				DataModelObjBase obj = iter.next();
				if (isObjectToSelect(parent, obj))
				{
					return obj;
				}
			}
		}
		return null;
	}
	
	/**
	 * @param parent
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	protected boolean isObjectToSelect(DataModelObjBase parent, DataModelObjBase obj) throws Exception
	{
		if (parent.getClass().equals(CollectionObject.class))
		{
			if (obj.getClass().equals(Determination.class))
			{
				return ((Determination )obj).getIsCurrent();
			}
			//just 
		}
		throw new Exception("Unsupported parent class " + parent.getClass().getName());
	}
	
	protected boolean isDatePartAccessor(String methodName)
	{
		return methodName.endsWith(DateAccessorQRI.DATEPART.NumericDay.toString())
			|| methodName.endsWith(DateAccessorQRI.DATEPART.NumericMonth.toString())
			|| methodName.endsWith(DateAccessorQRI.DATEPART.NumericYear.toString());		
	}
	
	protected String getDateFieldNameForDatePartAccessor(String methodName)
	{
		String strippee = null;
		if (methodName.endsWith(DateAccessorQRI.DATEPART.NumericDay.toString()))
		{
			strippee = DateAccessorQRI.DATEPART.NumericDay.toString();
		} else if (methodName.endsWith(DateAccessorQRI.DATEPART.NumericMonth.toString()))
		{
			strippee = DateAccessorQRI.DATEPART.NumericMonth.toString();
		} else if (methodName.endsWith(DateAccessorQRI.DATEPART.NumericYear.toString()))
		{
			strippee = DateAccessorQRI.DATEPART.NumericYear.toString();
		}
		return methodName.replace(strippee, "");
				
	}
	
	protected Integer getDatePart(Calendar object, String methodName) throws Exception
	{
		if (methodName.endsWith(DateAccessorQRI.DATEPART.NumericDay.toString()))
		{
			return object.get(Calendar.DAY_OF_MONTH);
		} else if (methodName.endsWith(DateAccessorQRI.DATEPART.NumericMonth.toString()))
		{
			return object.get(Calendar.MONTH) + 1;
		} else if (methodName.endsWith(DateAccessorQRI.DATEPART.NumericYear.toString()))
		{
			return object.get(Calendar.YEAR);
		}
		throw new Exception("DwcMapper.getDatePart: unable to process " + methodName);
	}
	
	protected DBFieldInfo getFieldInfo(DataModelObjBase object, String fieldName)
	{
		DBTableInfo tbl = DBTableIdMgr.getInstance().getInfoById(object.getTableId());
		return tbl.getFieldByName(fieldName);
	}
	/**
	 * @param object
	 * @param mapping
	 * @param isFormatted
	 * @param isTreeRank
	 * @param session
	 * @return
	 * @throws Exception
	 */
	protected Object getValueFromObject(DataModelObjBase object, String mapping, boolean isFormatted, 
			boolean isTreeRank, DataProviderSessionIFace session) throws Exception
	{
		if (!isFormatted && !isTreeRank)
		{
			String fieldName = mapping.split("\\.")[2];
			Vector<String> methNames = new Vector<String>();
			methNames.add("get" + fieldName.substring(0, 1).toUpperCase().concat(fieldName.substring(1)));
			if (isDatePartAccessor(methNames.get(0)))
			{
				methNames.add(getDateFieldNameForDatePartAccessor(methNames.get(0)));
			}
			boolean useDatePartAccessor = false;
			Method method = null;
			for (int m = 0; m < methNames.size(); m++)
			{
				String methodName = methNames.get(m);
				try 
				{
					method = object.getClass().getMethod(methodName);
					break;
				} catch (NoSuchMethodException ex)
				{
					useDatePartAccessor = true;
					continue;
				}
			}
			if (method == null)
			{
				throw new NoSuchMethodException(mapping);
			}
			//System.out.println("Getting a value: " + object + ", " + mapping + " = " + method.invoke(object));
			Object result = method.invoke(object);
			DBFieldInfo fi = getFieldInfo(object, fieldName);
			if (fi != null && !useDatePartAccessor)
			{
				if (fi.getFormatter() != null)
				{
					result = fi.getFormatter().formatToUI(result);
				} else if (Calendar.class.equals(fi.getDataClass()))
	            {
	            	Object date = result;
	            	if (date != null)
	            	{
	    				UIFieldFormatterIFace.PartialDateEnum precision = UIFieldFormatterIFace.PartialDateEnum.Full;
	    				
	    				if (fi.isPartialDate())
	    				{
	    					String methodName = "get" + StringUtils.capitalize(fi.getDatePrecisionName());
	    					Method precMethod = object.getClass().getMethod(methodName);
	    					Byte rawPrec = (Byte)precMethod.invoke(object);
	    		            if (rawPrec != null)
	    		            {
	    		            	precision = UIFieldFormatterIFace.PartialDateEnum.values()[rawPrec];
	    		            }
	    				}
	    				boolean isPartial = false;
	    				String formatName = "Date";
	    				if (precision.equals(UIFieldFormatterIFace.PartialDateEnum.Month))
	    				{
	    					isPartial = true;
	    					formatName = "PartialDateMonth";
	    				} else if (precision.equals(UIFieldFormatterIFace.PartialDateEnum.Year))
	    				{
	    					isPartial = true;
	    					formatName = "PartialDateYear";
	    				}
	    				for (UIFieldFormatterIFace formatter : UIFieldFormatterMgr.getInstance().getDateFormatterList(isPartial))
	    				{
	    					if (formatter.getName().equals(formatName))
	    					{
	    						result = formatter.getDateWrapper().format(((Calendar)date).getTime());
	    						break;
	    					}
	    				}
	            	}
	            }
			}
			if (useDatePartAccessor && result != null)
			{
				result = getDatePart((Calendar )result, methNames.get(0));
			}
			return result;
		}
		else
		{
			if (isTreeRank)
			{
				return getTreeRank((Treeable<?,?,?> )object, mapping, session);
			}
			else
			{
				return getFormatted(object, mapping, session);
			}
		}
	}
	
	/**
	 * @param object
	 * @param mapping
	 * @param session
	 * @return
	 * @throws Exception
	 */
	protected String getFormatted(DataModelObjBase object, String mapping, DataProviderSessionIFace session) throws Exception
	{
		//System.out.println("Getting a formatted/aggregated value: " + object + ", " + mapping);
		Object objects = getRelatedObjects(object, mapping);
		if (objects == null)
		{
			return null;
		}
		if (!Collection.class.isAssignableFrom(objects.getClass()))
		{
			//return DataObjFieldFormatMgr.getInstance().format(object, object.getClass());	
			return DataObjFieldFormatMgr.getInstance().format(objects, objects.getClass());	
		}
		Collection<?> objs = (Collection<?> )objects;
		if (objs.size() == 0)
		{
			return null; 
		}
		return DataObjFieldFormatMgr.getInstance().aggregate(objs, objs.iterator().next().getClass());
	}
	/**
	 * @param object
	 * @param mapping
	 * @param session
	 * @return
	 * @throws Exception
	 */
	protected String getTreeRank(Treeable<?, ?, ?> object, String mapping, DataProviderSessionIFace session) throws Exception
	{
		//System.out.println("Getting a tree rank: " + object + ", " + mapping);
		String tblName = object.getClass().getSimpleName().toLowerCase();
		String treeDefFld = object.getClass().getSimpleName().toLowerCase() + "TreeDefID";
		TreeDefIface<?,?,?> treeDef = (TreeDefIface<?,?,?> )session.get(object.getDefinition().getClass(), object.getDefinition().getTreeDefId());
		for (TreeDefItemIface<?,?,?> di : treeDef.getTreeDefItems())
		{
			if (mapping.endsWith(di.getName()))
			{
				String sql = "select name from " + tblName + " where " + treeDefFld + " = " + object.getDefinition().getTreeDefId()
					+ " and rankid = " + di.getRankId() + " and " + object.getNodeNumber() + " between NodeNumber and HighestChildNodeNumber";
				return BasicSQLUtils.querySingleObj(sql);
			}
					
		}
		return null;
	}
	/**
	 * @return number of concepts
	 */
	public int getConceptCount()
	{
		return concepts.size();
	}
	
	/**
	 * @param c
	 * @return concept c
	 */
	public MappingInfo getConcept(int c)
	{
		return concepts.get(c);
	}
		
	
	@SuppressWarnings("serial")
	public class MissingRecordException extends Exception
	{
		protected final Integer Id;
		protected final String mappingName;
		/**
		 * @param id
		 * @param mappingName
		 */
		public MissingRecordException(String msg, Integer id, String mappingName)
		{
			super(msg);
			Id = id;
			this.mappingName = mappingName;
		}
		/**
		 * @return the id
		 */
		public Integer getId()
		{
			return Id;
		}
		/**
		 * @return the mappingName
		 */
		public String getMappingName()
		{
			return mappingName;
		}
		
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		try
		{
			java.awt.Desktop.getDesktop().browse(java.net.URI.create("http://test.morphbank.net/?id=1000346"));
		} catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(-1);
		}
	}

}
