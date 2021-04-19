package org.mitre.hapifhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.parser.IParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.mitre.hapifhir.model.SubscriptionTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor
public class TopicListInterceptor {
    private final Logger myLogger = LoggerFactory.getLogger(TopicListInterceptor.class.getName());

    private FhirContext myCtx;
    private IParser jparser;
    private List<SubscriptionTopic> subscriptionTopics;

    private static final String TOPIC_LIST_EXT_URL = 
      "http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-subscription-topic-canonical-urls";

    /**
     * Create a new interceptor.
     * 
     * @param ctx - the fhir context to use
     * @param subscriptionTopics - list of subscription topics this server supports
     */
    public TopicListInterceptor(FhirContext ctx, List<SubscriptionTopic> subscriptionTopics) {
        this.myCtx = ctx;
        this.jparser = this.myCtx.newJsonParser();
        this.subscriptionTopics = subscriptionTopics;
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
        if (theRequest.getPathInfo().equals("/Subscription/$topic-list") && theRequest.getMethod().equals("GET")) {
            myLogger.info("Request received for $topic-list");
            try {
                handleTopicList(theResponse);
            } catch (Exception e) {
                myLogger.error("Exception: " + e.getMessage(), e);
            }
            return false;
        }
        return true;
    }

    /**
     * The handler to send the response of the operation.
     * 
     * @param theResponse - HttpServletResponse object
     * @throws IOException when unable to write response
     */
    public void handleTopicList(HttpServletResponse theResponse) throws IOException {
        Meta meta = new Meta();
        meta.addProfile(TOPIC_LIST_EXT_URL);

        Parameters topicList = new Parameters();
        topicList.setMeta(meta);
        for (SubscriptionTopic subscriptionTopic : this.subscriptionTopics) {
            ParametersParameterComponent parameter = new ParametersParameterComponent();
            parameter.setName(subscriptionTopic.getName());
            parameter.setValue(new CanonicalType(subscriptionTopic.getTopicUrl()));
            topicList.addParameter(parameter);
        }

        theResponse.setStatus(200);
        theResponse.setContentType("application/json");
        theResponse.setCharacterEncoding("UTF-8");
        PrintWriter out = theResponse.getWriter();
        out.print(jparser.setPrettyPrint(true).encodeResourceToString(topicList));
        out.flush();
    }
}
