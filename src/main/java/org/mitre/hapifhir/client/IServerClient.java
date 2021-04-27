package org.mitre.hapifhir.client;

import ca.uhn.fhir.rest.api.MethodOutcome;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

/**
 * IServerClient is an interface to perform CRUD operations on the HAPI
 * server. Since there are a variety of ways to do this depending on the
 * implementation of HAPI, this interface is used to abstract the implementation.
 * Furthermore, different implementations of this interface can use different
 * authorization schemes.
 */
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
