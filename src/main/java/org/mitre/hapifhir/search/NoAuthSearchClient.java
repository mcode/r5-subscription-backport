package org.mitre.hapifhir.search;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import org.hl7.fhir.r4.model.Bundle;

public class NoAuthSearchClient implements ISearchClient {
    
    private IGenericClient client;

    public NoAuthSearchClient(IGenericClient client) {
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
}
