package edu.ku.brc.specify.datamodel;

import java.util.*;




/**
 *        @hibernate.class
 *         table="collectionobjdef"
 *     
 */
public class CollectionObjDef  implements java.io.Serializable {

    // Fields    

     protected Integer collectionObjDefId;
     protected String name;
     protected DataType dataType;
     private Set catalogSeries;
     protected TaxonomyTreeDef taxonomyTreeDef;
     protected User user;
     private Set attrsDefs;


    // Constructors

    /** default constructor */
    public CollectionObjDef() {
    }
    
    /** constructor with id */
    public CollectionObjDef(Integer collectionObjDefId) {
        this.collectionObjDefId = collectionObjDefId;
    }
   
    
    

    // Property accessors

    /**
     * 
     */
    public Integer getCollectionObjDefId() {
        return this.collectionObjDefId;
    }
    
    public void setCollectionObjDefId(Integer collectionObjDefId) {
        this.collectionObjDefId = collectionObjDefId;
    }

    /**
     *      *            @hibernate.property
     *             column="Name"
     *             length="50"
     *         
     */
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    /**
     *      *            @hibernate.many-to-one
     *             not-null="true"
     *            @hibernate.column name="DataTypeID"
     *         
     */
    public DataType getDataType() {
        return this.dataType;
    }
    
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * 
     */
    public Set getCatalogSeries() {
        return this.catalogSeries;
    }
    
    public void setCatalogSeries(Set catalogSeries) {
        this.catalogSeries = catalogSeries;
    }

    /**
     *      *            @hibernate.one-to-one
     *             outer-join="auto"
     * 			cascade="delete"
     *         
     */
    public TaxonomyTreeDef getTaxonomyTreeDef() {
        return this.taxonomyTreeDef;
    }
    
    public void setTaxonomyTreeDef(TaxonomyTreeDef taxonomyTreeDef) {
        this.taxonomyTreeDef = taxonomyTreeDef;
    }

    /**
     *      *            @hibernate.many-to-one
     *             not-null="true"
     *            @hibernate.column name="userId"
     *         
     */
    public User getUser() {
        return this.user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }

    /**
     *      *            @hibernate.set
     *             lazy="true"
     *             inverse="true"
     *             cascade="none"
     *            @hibernate.collection-key
     *             column="AttrsDefID"
     *            @hibernate.collection-one-to-many
     *             class="edu.ku.brc.specify.datamodel.AttrsDef"
     *         
     */
    public Set getAttrsDefs() {
        return this.attrsDefs;
    }
    
    public void setAttrsDefs(Set attrsDefs) {
        this.attrsDefs = attrsDefs;
    }




}