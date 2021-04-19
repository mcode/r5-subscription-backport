package org.mitre.hapifhir.search;

import org.hl7.fhir.r4.model.Bundle;

public interface ISearchClient {
    
    /**
     * Search for resources matching criteria.
     * 
     * @param criteria - the criteria string e.g. "Subscription?active=true" 
     * @return the search bundle from the server
     */
    public Bundle searchOnCriteria(String criteria);
}
