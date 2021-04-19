package org.mitre.hapifhir.utils;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;

public class SubscriptionHelper {

    private static final String TOPIC_CANONICAL_EXT_URL = 
      "http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-topic-canonical";
    
    /**
     * Helper method to get the backport-topic-canonical url from the subscription.
     * @param subscription - the subscription
     * @return the backport-topic-canonical extension if found, otherwise null
     */
    public static String getTopicCanonical(Subscription subscription) {
        Extension topicCanonicalExtension = subscription.getExtensionByUrl(TOPIC_CANONICAL_EXT_URL);
        if (topicCanonicalExtension == null) {
            return null;
        }
        StringType value = (StringType) topicCanonicalExtension.getValue();
        return value.asStringValue();
    }

    /**
     * Helper method to get all criteria from subscription. That is the criteria property
     * and the list of _criteria supported by the backport IG.
     * 
     * @param subscription - the subscription resource to get criteria from
     * @return list of criteria strings
     */
    public static List<String> getCriteria(Subscription subscription) {
        List<String> criteria = new ArrayList<>();
        // put in the default criteria
        criteria.add(subscription.getCriteria());

        // TODO: get extra criteria from _criteria property
        // Property extraCriteriaProperty = subscription.getNamedProperty("_criteria");
        // List<Base> extraCriteriaList = extraCriteriaProperty.getValues();
        // for(Base extraCriteria : extraCriteriaList) {
        // }
        return criteria;
    }

    /**
     * Helper method to search the server by criteria.
     * 
     * @param client - the fhir client used to perform the search
     * @param criteria - the criteria string e.g. "Patient?id=123"
     * @return the search bundle from the server
     */
    public static Bundle searchOnCriteria(IGenericClient client, String criteria) {
        return client.search().byUrl(criteria)
            .returnBundle(Bundle.class)
            .execute();
    }
}
