package org.mitre.hapifhir.utils;

import ca.uhn.fhir.rest.api.RequestTypeEnum;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.UriType;
import org.mitre.hapifhir.model.ResourceTrigger.MethodCriteria;
import org.mitre.hapifhir.search.ISearchClient;

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
        UriType value = (UriType) topicCanonicalExtension.getValue();
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

        return criteria;
    }

    /**
     * Helper method to determine if the requestType matches any of the topic methodCriteria.
     * 
     * @param requestType - the current request type
     * @param methodCriteria - the topic method criteria
     * @param theResource - the resource from the request, used to check if a PUT is a CREATE
     * @return true if the requestType matches, false otherwise
     */
    public static boolean requestTypeMatches(RequestTypeEnum requestType, List<MethodCriteria> methodCriteria,
      IBaseResource theResource) {
        if (methodCriteria.contains(MethodCriteria.DELETE) && requestType.equals(RequestTypeEnum.DELETE)) {
            return true;
        } else if (methodCriteria.contains(MethodCriteria.UPDATE) && requestType.equals(RequestTypeEnum.PUT)) {
            // According to the valueset an UPDATE is an update or create
            // https://build.fhir.org/valueset-interaction-trigger.html
            return true;
        } else if (methodCriteria.contains(MethodCriteria.CREATE)) {
            if (requestType.equals(RequestTypeEnum.POST)) {
                return true;
            } else if (requestType.equals(RequestTypeEnum.PUT)) {
                return theResource.getMeta().getVersionId().equals("1");
            }
        }
        return false;
    }

    /**
     * Helper method to determine if a resource matches any of the criteria from a Subscription.
     * Uses search client to get by criteria and validate the resource is in the Bundle.
     * 
     * @param subscription - the subscription to check criteria from
     * @param theResource - the resource to check against
     * @param searchClient - search client
     * @return true if the resource matches at least one criteria, false otherwise
     */
    public static boolean matchesCriteria(Subscription subscription, Resource theResource, 
      ISearchClient searchClient) {
        for (String criteria : SubscriptionHelper.getCriteria(subscription)) {
            String searchCriteria = criteria + "&_id=" + theResource.getIdElement().getIdPart();
            Bundle searchBundle = searchClient.searchOnCriteria(searchCriteria);
            for (BundleEntryComponent entry : searchBundle.getEntry()) {
                Resource resource = entry.getResource();
                if (resource.getIdElement().getIdPart().equals(theResource.getIdElement().getIdPart())
                    && resource.fhirType().equals(theResource.fhirType())) {
                    return true;
                }
            }
        }

        return false;
    }
}
