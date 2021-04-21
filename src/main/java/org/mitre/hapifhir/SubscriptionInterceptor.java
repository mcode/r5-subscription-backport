package org.mitre.hapifhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.mitre.hapifhir.model.ResourceTrigger;
import org.mitre.hapifhir.model.ResourceTrigger.MethodCriteria;
import org.mitre.hapifhir.model.SubscriptionTopic;
import org.mitre.hapifhir.model.SubscriptionTopic.NotificationType;
import org.mitre.hapifhir.search.ISearchClient;
import org.mitre.hapifhir.utils.CreateNotification;
import org.mitre.hapifhir.utils.SubscriptionHelper;
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
    private ISearchClient searchClient;
    private List<SubscriptionTopic> subscriptionTopics;

    /**
     * Create a new interceptor.
     * 
     * @param url - the server base url
     * @param ctx - the fhir context to use
     * @param searchClient - the client used to search the server
     * @param subscriptionTopics - list of subscription topics this server supports
     */
    public SubscriptionInterceptor(String url, FhirContext ctx, ISearchClient searchClient,
      List<SubscriptionTopic> subscriptionTopics) {
        this.baseUrl = url;
        this.myCtx = ctx;
        this.searchClient = searchClient;
        this.subscriptionTopics = subscriptionTopics;
        this.jparser = this.myCtx.newJsonParser();
    }

    /**
     * Override the incomingRequestPostProcessed method, which is called for
     * each request after processing is done.
     * NOTE: this may not be the best pointcut
     * 
     * @param theRequestDetails - HAPI interceptor request details
     * @param theResource - the resource being returned by the request
     * @return true when complete
     */
    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, IBaseResource theResource) {
        // Determine which SubscriptionTopic, if any, should be triggered
        SubscriptionTopic subscriptionTopic = getSubscriptionTopic(theRequestDetails, theResource);
        if (subscriptionTopic != null) { 
            myLogger.info("Checking subscriptions for topic " + subscriptionTopic.getName());
            // Find all subscriptions to be notified
            Resource resource = (Resource) theResource;
            String topicUrl = subscriptionTopic.getTopicUrl();
            for (Subscription subscription: getSubscriptionsToNotify(topicUrl, resource)) {
                Bundle notification = CreateNotification.createResourceNotification(subscription,
                  Collections.singletonList(resource), this.baseUrl, topicUrl,
                  NotificationType.EVENT_NOTIFICATION);
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
        RequestTypeEnum requestType = theRequestDetails.getRequestType();
        ResourceType resourceType = ResourceType.fromCode(theResource.fhirType());

        for (SubscriptionTopic subscriptionTopic : this.subscriptionTopics) {
            // TODO: I believe triggers of the same resourceType indicate AND while
            //  triggers of different resourceType indicates OR
            boolean isTopicMatch = false;
            for (ResourceTrigger resourceTrigger : subscriptionTopic.getResourceTriggers()) {
                // If requestType does not match methodCriteria there is no resourceTrigger match
                if (!requestTypeMatches(requestType, resourceTrigger.getMethodCriteria(), theResource)) {
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
     * Helper method to determine if the requestType matches any of the topic methodCriteria.
     * 
     * @param requestType - the current request type
     * @param methodCriteria - the topic method criteria
     * @param theResource - the resource from the request, used to check if a PUT is a CREATE
     * @return true if the requestType matches, false otherwise
     */
    private boolean requestTypeMatches(RequestTypeEnum requestType, List<MethodCriteria> methodCriteria,
      IBaseResource theResource) {
        if (methodCriteria.contains(MethodCriteria.DELETE) && requestType.equals(RequestTypeEnum.DELETE)) {
            return true;
        } else if (methodCriteria.contains(MethodCriteria.UPDATE) && requestType.equals(RequestTypeEnum.PUT)) {
            return !theResource.getMeta().getVersionId().equals("1");
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
     * Helper function to get all subscriptions from the server which
     * need to be notified. Matches topic and criteria.
     * 
     * @param topicUrl - the topic url to find subscriptions for
     * @param theResource - the triggering resource used to check subscription criteria
     * @return list of Subscription resource
     */
    private List<Subscription> getSubscriptionsToNotify(String topicUrl, Resource theResource) {
        myLogger.info("Checking all active subscriptions for topic " + topicUrl);
        // TODO: set status to active when accepting subscriptions
        Bundle results = this.searchClient.searchOnCriteria("Subscription");
        List<Subscription> subscriptions = new ArrayList<>(); 
        for (BundleEntryComponent entry: results.getEntry()) {
            Resource resource = entry.getResource();
            if (!resource.getResourceType().equals(ResourceType.Subscription)) {
                continue;
            }

            Subscription subscription = (Subscription) resource;

            // Check Subscription topic extension, if not equal skip subscription
            if (!SubscriptionHelper.getTopicCanonical(subscription).equals(topicUrl)) {
                continue;
            }

            // Check at least one Subscription criteria matches resource, if not skip subscription
            if (!SubscriptionHelper.matchesCriteria(subscription, theResource, this.searchClient)) {
                continue;
            }

            // If we get this far the topic and criteria matches
            subscriptions.add(subscription);
        }
        myLogger.info("Found " + subscriptions.size() + " subscriptions to notify.");
        return subscriptions;
    }

    /**
     * Send the notification to the subscriber.
     * 
     * @param subscription - the subscription resource the notification is for
     * @param notification - the notification bundle to send
     */
    private void sendNotification(Subscription subscription, Bundle notification) {
        String subscriptionId = subscription.getIdElement().getIdPart();
        String endpoint = subscription.getChannel().getEndpoint();

        if (endpoint == null) {
            myLogger.error("UnsupportedChannelTypeException: Subscription/" + subscriptionId 
                + " must be rest-hook and include channel.endpoint");
            return;
        }

        myLogger.info("Sending notification for Subscription/" + subscriptionId + " to " + endpoint);

        try {
            StringEntity data = new StringEntity(jparser.setPrettyPrint(true)
              .encodeResourceToString(notification));
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setEntity(data);
            httpPost.addHeader("Content-type", "application/json");
            for (StringType header : subscription.getChannel().getHeader()) {
                String headerString = header.asStringValue();
                String[] headerParts = headerString.split(": ", 2);
                httpPost.addHeader(headerParts[0], headerParts[1]);
            }
            HttpClient httpClient = HttpClients.createDefault();
            httpClient.execute(httpPost);
        } catch (UnsupportedEncodingException e) {
            myLogger.error("UnsupportedEncodingException sending notification for Subscription/"
                + subscriptionId, e);
        } catch (ClientProtocolException e) {
            myLogger.error("ClientProtocolException sending notification for Subscription/" + subscriptionId, e);
        } catch (IOException e) {
            myLogger.error("IOException sending notification for Subscription/" + subscriptionId, e);
        } catch (Exception e) {
            myLogger.info("Error sending notification for Subscription/" + subscriptionId);
        }
    }
}
