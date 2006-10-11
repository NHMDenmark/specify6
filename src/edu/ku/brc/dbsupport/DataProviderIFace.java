/**
* Copyright (C) 2006  The University of Kansas
*
* [INSERT KU-APPROVED LICENSE TEXT HERE]
*
*/
package edu.ku.brc.dbsupport;

public interface DataProviderIFace
{
    public void evict(Class clsObject);
    
    public void shutdown();
    
    public DataProviderSessionIFace getCurrentSession();
    
    public DataProviderSessionIFace createSession();
    
}
