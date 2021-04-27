package org.mitre.hapifhir.client;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

public class NoAuthServerClient implements IServerClient {
    
    private IGenericClient client;

    public NoAuthServerClient(IGenericClient client) {
        this.client = client;
    }

    /**
     * Search for resources matching criteria on the server defined by the client.
     * 
     * @param criteria - the criteria string e.g. "Subscription?active=true" 
     * @return the search bundle from the server
     */
    public Bundle searchOnCriteria(String criteria) {
        return client.search().byUrl(criteria)
            .returnBundle(Bundle.class)
            .execute();
    }

    /**
     * Updates a resource on the server defined by the client.
     * 
     * @param resource - the updated resource
     * @return method outcome
     */
    public MethodOutcome updateResource(IBaseResource resource) {
        return client.update().resource(resource)
            .execute();
    }
}
