package org.mitre.hapifhir;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor
public class TopicListInterceptor {
    private final Logger myLogger = LoggerFactory.getLogger(TopicListInterceptor.class.getName());

    /**
     * Override the incomingRequestPreProcessed method, which is called
     * for each incoming request before any processing is done.
     */
    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public boolean incomingRequestPreProcessed(HttpServletRequest theRequest, HttpServletResponse theResponse) {
        String[] parts = theRequest.getRequestURL().toString().split("/");
        if (parts[parts.length - 1].equals("$topic-list") 
              && parts[parts.length - 2].equals("Subscription") 
              && theRequest.getMethod().equals("GET")) {
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
        theResponse.setStatus(200);
        PrintWriter out = theResponse.getWriter();
        theResponse.setContentType("application/json");
        theResponse.setCharacterEncoding("UTF-8");
        // TODO: generate this based on SubscriptionTopics in the server
        out.print("{}");
        out.flush();
    }
}
