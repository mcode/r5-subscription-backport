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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mitre.hapifhir.search.ISearchClient;
import org.mitre.hapifhir.utils.CreateNotification;
import org.mitre.hapifhir.utils.SubscriptionHelper;

@Interceptor
public class SubscriptionStatusInterceptor {
    private final Logger myLogger = LoggerFactory.getLogger(SubscriptionStatusInterceptor.class.getName());

    private FhirContext myCtx;
    private IParser jparser;
    private ISearchClient searchClient;

    /**
     * Create a new interceptor.
     *
     * @param ctx - the fhir context to use
     * @param searchClient - the client used to search the server
     */
    public SubscriptionStatusInterceptor(FhirContext ctx, ISearchClient searchClient) {
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
        Pattern p = Pattern.compile("/Subscription/\w./\$status");
        Matcher m = p.matcher(theRequest.getPathInfo());

        if (theRequest.getPathInfo().equals("/Subscription/\$status") && theRequest.getMethod().equals("GET")) {
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
    private Subscription getSubscription(HttpServletResponse theResponse, String id) {
        searchClient.searchOnCriteria("Subscription/" + id);
        for (BundleEntryComponent entry: results.getEntry()) {
            Resource resource = entry.getResource();
            if (resource.getResourceType().equals(ResourceType.Subscription)) {
                return (Subscription) resource;
            }
        }
        return null;
    }
    private Parameters makeParameter(Subscription subscription) {
        Parameters status = new Parameters();
        Meta meta = new Meta();
        meta.addProfile("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-subscriptionstatus");
        status.setMeta(meta);
        return status;
    }


}
