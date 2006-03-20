package edu.ku.brc.specify.datamodel;

import java.util.*;




/**
 * RecordSet generated by hbm2java
 */
public class RecordSet  implements java.io.Serializable {

    // Fields    

     private Long recordSetID;
     private String name;
     private Integer tableId;
     private Date timestampModified;
     private Date timestampCreated;
     private Set items;
     protected User owner;


    // Constructors

    /** default constructor */
    public RecordSet() {
    }
    
    /** constructor with id */
    public RecordSet(Long recordSetID) {
        this.recordSetID = recordSetID;
    }
   
    
    

    // Property accessors

    /**
     * 
     */
    public Long getRecordSetID() {
        return this.recordSetID;
    }
    
    public void setRecordSetID(Long recordSetID) {
        this.recordSetID = recordSetID;
    }

    /**
     * 
     */
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     */
    public Integer getTableId() {
        return this.tableId;
    }
    
    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }

    /**
     *      *            @hibernate.property
     *             column="TimestampModified"
     *             length="23"
     *             not-null="true"
     *         
     */
    public Date getTimestampModified() {
        return this.timestampModified;
    }
    
    public void setTimestampModified(Date timestampModified) {
        this.timestampModified = timestampModified;
    }

    /**
     *      *            @hibernate.property
     *             column="TimestampCreated"
     *             length="23"
     *             update="false"
     *             not-null="true"
     *         
     */
    public Date getTimestampCreated() {
        return this.timestampCreated;
    }
    
    public void setTimestampCreated(Date timestampCreated) {
        this.timestampCreated = timestampCreated;
    }

    /**
     * 
     */
    public Set getItems() {
        return this.items;
    }
    
    public void setItems(Set items) {
        this.items = items;
    }

    /**
     *      *            @hibernate.many-to-one
     *             not-null="true"
     *            @hibernate.column name="UserID"
     *         
     */
    public User getOwner() {
        return this.owner;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }




}