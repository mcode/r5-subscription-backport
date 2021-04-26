package org.mitre.hapifhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;

import org.mitre.hapifhir.client.IServerClient;
import org.mitre.hapifhir.utils.CreateNotification;
import org.mitre.hapifhir.utils.SubscriptionHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor
public class SubscriptionStatusInterceptor {
    private final Logger myLogger = LoggerFactory.getLogger(SubscriptionStatusInterceptor.class.getName());

    private FhirContext myCtx;
    private IParser jparser;
    private IServerClient searchClient;

    /**
     * Create a new interceptor.
     *
     * @param ctx - the fhir context to use
     * @param searchClient - the client used to search the server
     */
    public SubscriptionStatusInterceptor(FhirContext ctx, IServerClient searchClient) {
        this.myCtx = ctx;
        this.jparser = this.myCtx.newJsonParser();
        this.searchClient = searchClient;
    }

    /**
     * Override the incomingRequestPreProcessed method, which is called
     * for each incoming request before any processing is done.
     *
     * @param theRequest - the HttpServletRequest
     * @param theResponse - the HttpServletResponse
     * @return true when processing should continue as normal, false when interceptor is activated
     */
    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public boolean incomingRequestPreProcessed(HttpServletRequest theRequest, HttpServletResponse theResponse) {
        Pattern p = Pattern.compile("/Subscription/\\w./\\$status");
        Matcher m = p.matcher(theRequest.getPathInfo());

        if (theRequest.getPathInfo().equals("/Subscription/$status") && theRequest.getMethod().equals("GET")) {
            myLogger.info("Request received for $status");

        } else if (m.matches() && theRequest.getMethod().equals("GET")) {
            String id = theRequest.getRequestURL().toString().split("/")[1];
            myLogger.info("Request received for $status on subscription with id " + id);
            try {
                Subscription sub = getSubscription(theResponse, id);
                Parameters param = makeParameter(sub);
                // TODO: Update theResponse to use the parameter as the body
            } catch (Exception e) {
                myLogger.error("Exception: " + e.getMessage(), e);
            }
            return false;
        }
        return true;
    }

    /**
     * Retrieves the subscription for the corresponding resource id
     *
     * @param id - the id of the resource to retrieve
     * @return the subscription resource if it exists or null otherwise
     */
    private Subscription getSubscription(String id) {
        Bundle results = searchClient.searchOnCriteria("Subscription/" + id);
        for (BundleEntryComponent entry: results.getEntry()) {
            Resource resource = entry.getResource();
            if (resource.getResourceType().equals(ResourceType.Subscription)) {
                return (Subscription) resource;
            }
        }
        return null;
    }

    /**
     * Create and populate the parameter resource for the subscription
     *
     * @param subscription - the subscription to base the parameter on
     * @return the status parameter
     */
    private Parameters makeParameter(Subscription subscription) {
        // TODO: Update Parameter to use real values from the subscription
        Parameters status = new Parameters();
        Meta meta = new Meta();
        meta.addProfile("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-subscriptionstatus");
        status.setMeta(meta);

        ParametersParameterComponent topicParam = new ParametersParameterComponent();
        topicParam.setName("topic");
        topicParam.setValue(new CanonicalType("http://hl7.org/SubscriptionTopic/admission"));
        status.addParameter(topicParam);

        ParametersParameterComponent typeParam = new ParametersParameterComponent();
        typeParam.setName("type");
        typeParam.setValue(new CanonicalType("event-notification"));
        status.addParameter(typeParam);

        ParametersParameterComponent statusParam = new ParametersParameterComponent();
        statusParam.setName("status");
        statusParam.setValue(new StringType(subscription.getStatus().toString()));
        status.addParameter(statusParam);

        ParametersParameterComponent startParam = new ParametersParameterComponent();
        startParam.setName("events-since-subscription-start");
        startParam.setValue(new IntegerType(310));
        status.addParameter(startParam);

        ParametersParameterComponent eventParam = new ParametersParameterComponent();
        eventParam.setName("events-in-notification");
        eventParam.setValue(new IntegerType(1));
        status.addParameter(eventParam);

        return status;
    }


}
