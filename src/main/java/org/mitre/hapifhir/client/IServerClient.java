package org.mitre.hapifhir.client;

import ca.uhn.fhir.rest.api.MethodOutcome;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

public interface IServerClient {
    
    /**
     * Search for resources matching criteria.
     * 
     * @param criteria - the criteria string e.g. "Subscription?active=true" 
     * @return the search bundle from the server
     */
    public Bundle searchOnCriteria(String criteria);

    /**
     * Update the resource.
     * 
     * @param resource - the updated resource
     * @return method outcome
     */
    public MethodOutcome updateResource(IBaseResource resource);
}
