package org.mitre.hapifhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Subscription;
import org.mitre.hapifhir.ResourceTrigger.MethodCriteria;
import org.mitre.hapifhir.SubscriptionTopic.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * R5 Backport Subscription Interceptor which checks every request
 * to see if a notification should be sent.
 */
@Interceptor
public class SubscriptionInterceptor {
    private final Logger myLogger = LoggerFactory.getLogger(SubscriptionInterceptor.class.getName());

    private String baseUrl;
    private IParser jparser;
    private FhirContext myCtx;
    private IGenericClient client;

    /**
     * Create a new interceptor.
     * 
     * @param url - the server base url
     * @param ctx - the fhir context to use.
     */
    public SubscriptionInterceptor(String url, FhirContext ctx) {
        this.baseUrl = url;
        this.myCtx = ctx;
        this.client = this.myCtx.newRestfulGenericClient(this.baseUrl + "/fhir");
        this.jparser = this.myCtx.newJsonParser();
    }

    /**
     * Override the incomingRequestPostProcessed method, which is called for
     * each request after processing is done.
     * NOTE: this may not be the best pointcut
     * 
     * @param theRequestDetails - HAPI interceptor request details
     * @param theResource - the resource being returned by the request
     */
    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, IBaseResource theResource) {
        // Determine which SubscriptionTopic, if any, should be triggered
        SubscriptionTopic subscriptionTopic = getSubscriptionTopic(theRequestDetails, theResource);
        if (subscriptionTopic != null) { 
            // Find all subscriptions to be notified
            for (Subscription subscription: getAllSubscriptions(subscriptionTopic.getTopicUrl())) {
                Bundle notification = getNotification(subscription);
                if (notification != null) {
                    sendNotification(subscription, notification);
                }
            }
        }

        return true;
    }

    /**
     * Find the SubscriptionTopic, if any which is triggered by this request.
     * 
     * @param theRequestDetails - HAPI interceptor request details
     * @param theResource - the resource being returned by the request
     * @return SubscriptionTopic which matches the current request, or null
     */
    private SubscriptionTopic getSubscriptionTopic(RequestDetails theRequestDetails, IBaseResource theResource) {
        List<SubscriptionTopic> serverSubscriptionTopics = new ArrayList<>();

        RequestTypeEnum requestType = theRequestDetails.getRequestType();
        ResourceType resourceType = ResourceType.fromCode(theResource.fhirType());

        for (SubscriptionTopic subscriptionTopic : serverSubscriptionTopics) {
            // TODO: I believe triggers of the same resourceType indicate AND while
            //  triggers of different resourceType indicates OR
            boolean isTopicMatch = false;
            for (ResourceTrigger resourceTrigger : subscriptionTopic.getResourceTriggers()) {
                // If requestType does not match methodCriteria there is no resourceTrigger match
                if (!requestTypeMatches(requestType, resourceTrigger.getMethodCriteria())) {
                    continue;
                }

                // If resourceType does not match there is no resourceTrigger match
                if (!resourceType.equals(resourceTrigger.getResourceType())) {
                    continue;
                }

                // If we get here all criteria is met so the topic is a match
                isTopicMatch = true;
            }

            if (isTopicMatch) {
                return subscriptionTopic;
            }
        }

        // None of the subscription topics match
        return null;
    }

    /**
     * Helper method to determine if the requestType matches the topic methodCriteria.
     * 
     * @param requestType - the current request type
     * @param methodCriteria - the topic method criteria
     * @return true if the requestType matches, false otherwise
     */
    private boolean requestTypeMatches(RequestTypeEnum requestType, MethodCriteria methodCriteria) {
        if (methodCriteria.equals(MethodCriteria.DELETE) && requestType.equals(RequestTypeEnum.DELETE)) {
            return true;
        } else if (methodCriteria.equals(MethodCriteria.UPDATE) && requestType.equals(RequestTypeEnum.PUT)) {
            return true;
        } else if (methodCriteria.equals(MethodCriteria.CREATE)) {
            if (requestType.equals(RequestTypeEnum.POST)) {
                return true;
            } else if (requestType.equals(RequestTypeEnum.PUT)) {
                // TODO: Could be a CREATE or an UPDATE check version history
            }
        }
        return false;
    }

    /**
     * Helper function to get all subscriptions from the server which
     * match the subscription topic.
     * 
     * @param topicUrl - the topic url to find subscriptions for
     * @return list of Subscription resource
     */
    private List<Subscription> getAllSubscriptions(String topicUrl) {
        myLogger.info("Checking all active subscriptions for topic " + topicUrl);
        // Only check the criteria on active subscriptions
        Bundle results = searchOnCriteria("/Subscription?status=active");
        List<BundleEntryComponent> entries = results.getEntry();
        List<Subscription> subscriptions = new ArrayList<>(); 
        for (BundleEntryComponent entry: entries) {
            try {
                Resource resource = entry.getResource();
                if (resource.fhirType().equals("Subscription")) {
                    subscriptions.add((Subscription) resource);
                }
            } catch (Exception ex) {
                myLogger.info("Failed to parse subscription");
            }
        }
        return subscriptions;
    }

    /**
     * Gets the notification if the resource for the subscription was updated in
     * the last 15 seconds. This is due to the pointcut used we must delay
     * TODO: fix this class so we do not need to use this hackish approach
     * 
     * @param subscription - the subscription resource to build the notification bundle for
     * @return the notification bundle or null on error
     */
    private Bundle getNotification(Subscription subscription) {
        List<String> criteriaList = getCriteria(subscription);
        List<Resource> resources = new ArrayList<>();
        for (String criteria : criteriaList) {
            Bundle searchBundle = searchOnCriteria(criteria);
            Date now = new Date(System.currentTimeMillis());
            for (BundleEntryComponent entry: searchBundle.getEntry()) {
                Resource resource = entry.getResource();

                InstantType lastUpdated = resource.getMeta().getLastUpdatedElement();
                lastUpdated.add(Calendar.SECOND, 15);
                if (lastUpdated.after(now)) {
                    myLogger.info("Resource found within 15 seconds");
                    resources.add(resource);
                }
            }
        }
        if (!resources.isEmpty()) {
            return CreateNotification.createResourceNotification(subscription, resources, baseUrl, "topicUrl",
                NotificationType.EVENT_NOTIFICATION);
        }

        return null;
    }

    /**
     * Send the notification to the subscriber.
     * 
     * @param subscription - the subscription resource the notification is for
     * @param notification - the notification bundle to send
     */
    private void sendNotification(Subscription subscription, Bundle notification) {
        String endpoint = subscription.getChannel().getEndpoint();
        myLogger.info("Sending notification for Subscription/" + subscription.getId() + " to " + endpoint);

        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(endpoint);
            // TODO: Add headers from the subscription
            StringEntity data = new StringEntity(jparser.setPrettyPrint(true)
              .encodeResourceToString(notification));
            httpPost.setEntity(data);
            httpPost.setHeader("Content-type", "application/json");
            httpClient.execute(httpPost);
        } catch (UnsupportedEncodingException e) {
            myLogger.error("UnsupportedEncodingException sending notification", e);
        } catch (ClientProtocolException e) {
            myLogger.error("ClientProtocolException sending notification", e);
        } catch (IOException e) {
            myLogger.error("IOException sending notification", e);
        } catch (Exception e) {
            myLogger.info("Error sending notification");
        }
    }

    /**
     * Helper method to get all criteria from subscription. That is the criteria property
     * and the list of _criteria supported by the backport IG.
     * 
     * @param subscription - the subscription resource to get criteria from
     * @return list of criteria strings
     */
    private List<String> getCriteria(Subscription subscription) {
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
     * @param criteria - the criteria string e.g. "Patient?id=123"
     * @return the search bundle from the server
     */
    private Bundle searchOnCriteria(String criteria) {
        return client.search().byUrl(criteria)
            .returnBundle(Bundle.class)
            .execute();
    }
}
