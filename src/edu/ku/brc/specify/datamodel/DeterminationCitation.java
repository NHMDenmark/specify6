/* Copyright (C) 2020, Specify Collections Consortium
 * 
 * Specify Collections Consortium, Biodiversity Institute, University of Kansas,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA, support@specifysoftware.org
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.specify.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

/**

 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@org.hibernate.annotations.Proxy(lazy = false)
@Table(name = "determinationcitation", uniqueConstraints = { 
        @UniqueConstraint(columnNames = { "ReferenceWorkID", "DeterminationID" }) 
})
@org.hibernate.annotations.Table(appliesTo="determinationcitation", indexes =
    {   
        @Index (name="DetCitColMemIDX", columnNames={"CollectionMemberID"})
    })
public class DeterminationCitation extends CollectionMember implements java.io.Serializable, Cloneable
{

    // Fields    

     protected Integer       determinationCitationId;
     protected String        remarks;
     protected ReferenceWork referenceWork;
     protected Determination determination;


    // Constructors

    /** default constructor */
    public DeterminationCitation() 
    {
        //
    }
    
    /** constructor with id */
    public DeterminationCitation(Integer determinationCitationId) {
        this.determinationCitationId = determinationCitationId;
    }
   
    
    

    // Initializer
    @Override
    public void initialize()
    {
        super.init();
        determinationCitationId = null;
        remarks = null;
        referenceWork = null;
        determination = null;
    }
    // End Initializer

    // Property accessors

    /**
     * 
     */
    @Id
    @GeneratedValue
    @Column(name = "DeterminationCitationID", unique = false, nullable = false, insertable = true, updatable = true)
    public Integer getDeterminationCitationId() {
        return this.determinationCitationId;
    }

    /**
     * Generic Getter for the ID Property.
     * @returns ID Property.
     */
    @Transient
    @Override
    public Integer getId()
    {
        return this.determinationCitationId;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getDataClass()
     */
    @Transient
    @Override
    public Class<?> getDataClass()
    {
        return DeterminationCitation.class;
    }
    
    public void setDeterminationCitationId(Integer determinationCitationId) {
        this.determinationCitationId = determinationCitationId;
    }

    /**
     * 
     */
    @Lob
    @Column(name = "Remarks", length = 4096)
    public String getRemarks() {
        return this.remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    /**
     *      * ID of the publication citing the determination
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "ReferenceWorkID", unique = false, nullable = false, insertable = true, updatable = true)
    public ReferenceWork getReferenceWork() {
        return this.referenceWork;
    }
    
    public void setReferenceWork(ReferenceWork referenceWork) {
        this.referenceWork = referenceWork;
    }

    /**
     *      * Determination being cited
     */
    @ManyToOne(cascade = {}, fetch = FetchType.LAZY)
    @JoinColumn(name = "DeterminationID", unique = false, nullable = false, insertable = true, updatable = true)
    public Determination getDetermination() {
        return this.determination;
    }
    
    public void setDetermination(Determination determination) {
        this.determination = determination;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentTableId()
     */
    @Override
    @Transient
    public Integer getParentTableId()
    {
        return Determination.getClassTableId();
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#getParentId()
     */
    @Override
    @Transient
    public Integer getParentId()
    {
        return determination != null ? determination.getId() : null;
    }

    /* (non-Javadoc)
     * @see edu.ku.brc.ui.forms.FormDataObjIFace#getTableId()
     */
    @Override
    @Transient
    public int getTableId()
    {
        return getClassTableId();
    }
    
    /**
     * @return the Table ID for the class.
     */
    public static int getClassTableId()
    {
        return 38;
    }
    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.datamodel.DataModelObjBase#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        DeterminationCitation obj = (DeterminationCitation)super.clone();
        obj.determinationCitationId = null;
        obj.determination           = null;
        
        return obj;
    }
}
