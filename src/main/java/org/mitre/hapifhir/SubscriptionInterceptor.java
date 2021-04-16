package org.mitre.hapifhir;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;

import ca.uhn.fhir.context.FhirContext;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.*;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.instance.model.api.*;

import ca.uhn.fhir.rest.client.api.*;
import ca.uhn.fhir.parser.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mitre.hapifhir.utils.RequestHandler;
import org.mitre.hapifhir.utils.JSONWrapper;

@Interceptor
public class SubscriptionInterceptor {
   private final Logger myLogger = LoggerFactory.getLogger(SubscriptionInterceptor.class.getName());

   private FhirContext myCtx;

   private String baseUrl;

   private IGenericClient client;

   private RequestHandler requestHandler;
   private IParser jparser;
   private JSONParser parser;

   /**
    * Constructor using a specific logger
    */
   public SubscriptionInterceptor(String url, FhirContext ctx) {
      configure(url, ctx);
   }

   private void configure(String url, FhirContext ctx) {
        baseUrl = url;
        myCtx = ctx;
        client = myCtx.newRestfulGenericClient(baseUrl + "/fhir");
        requestHandler = new RequestHandler();
        jparser = myCtx.newJsonParser();
        parser = new JSONParser();
   }

   public void setBaseUrl(String url) {
      baseUrl = url;
   }

   /*
    *   Searches on Subscriptions based on the IDs that are provided. The status are then created
    *   based on those subscriptions
    */

   /**
    * Override the incomingRequestPostProcessed method, which is called for
    * each request after processing is done.
    *
    * NOTE: this may not be the best pointcut
    */
   @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
   public boolean incomingRequestPostProcessed(HttpServletRequest theRequest, HttpServletResponse theResponse) {
     String[] parts = theRequest.getRequestURL().toString().split("/");
     // Here is where the Subscription Topic should be evaluated
     // TODO: replace this with a generic subscription topic engine
     if (theRequest.getMethod().equals("PUT")
        || theRequest.getMethod().equals("POST")
        && !parts[parts.length - 1].equals("Subscription")
        && !parts[parts.length - 2].equals("Subscription")
        && parts[parts.length - 1].equals("Task")) { // Server only checking Task resources
         myLogger.info("Checking active subscriptions for potential matches");
         for (JSONWrapper subscription: getAllSubscriptions()) {
            String notification = getNotification(subscription);
            if (!notification.equals("")) {
                sendNotification(subscription, notification);
            }
         }
         return true;
     }
     return true;
  }

  /**
   * Helper function to get all subscriptions from the server
   * TODO: replace this with a getAllSubscriptionsByTopic(topic) method
   */
  public List<JSONWrapper> getAllSubscriptions() {
      myLogger.info("Checking all active subscriptions");
      // Only check the criteria on active subscriptions
      Bundle results = searchOnCriteria("/Subscription?status=active");
      List<Bundle.BundleEntryComponent> subs = results.getEntry();
      List<JSONWrapper> retVal=new ArrayList<JSONWrapper>(); // populate this
      for (Bundle.BundleEntryComponent sub: subs) {
          try {
            JSONWrapper subscription = new JSONWrapper(parser.parse(jparser.encodeResourceToString((IBaseResource)sub.getResource())));
            retVal.add(subscription);
          } catch (Exception ex) {
              myLogger.info("Failed to parse subscription");
          }
      }
      return retVal;
  }

  /**
   * Gets the notification if the resource for the subsvription was updated in
   * the last 15 seconds. This is due to the pointcut used we must delay
   */
  private String getNotification(JSONWrapper subscription) {
      myLogger.info(subscription.toString());
      List<String> criteriaList = getCriteria(subscription);
      List<String> resources = new ArrayList<>();
      for (String c : criteriaList) {
          Bundle r = searchOnCriteria(c);
          for (Bundle.BundleEntryComponent e: r.getEntry()) {
              myLogger.info("TIME STAMP");
              InstantType lastUpdated = InstantType.withCurrentTime();

              String resource = jparser.encodeResourceToString((IBaseResource)e.getResource());
              myLogger.info(resource);
              boolean addResource = false;
              try {
                JSONWrapper rjw = new JSONWrapper(parser.parse(resource));
                InstantType lastUpdated2 = new InstantType(rjw.get("meta").get("lastUpdated").toString());
                lastUpdated2.add(Calendar.SECOND, 15);
                Date now = new Date(System.currentTimeMillis());
                myLogger.info(lastUpdated2.toString());
                if (lastUpdated2.after(now)) {
                    myLogger.info("Resource found within 15 seconds");
                    addResource = true;
                } else {
                    myLogger.info("Resource found but old resource");
                }
              } catch(Exception ex) {
                  myLogger.info(ex.toString());
              }
              myLogger.info(lastUpdated.toString());
              if (addResource) {
                  resources.add(jparser.encodeResourceToString((IBaseResource)e.getResource()));
              }
          }
      }
      String notification = "";
      if (resources.size() > 0) {
          notification = CreateNotification.createResourceNotification(subscription.toString(), resources, baseUrl + "/fhir/Subscription/admission/$status");
      }
      return notification;
  }

  /**
   * Handle sending the notification
   */
  private String sendNotification(JSONWrapper subscription, String notification) {
      myLogger.info("SENDING STUFF");
      String endpoint = subscription.get("channel").get("endpoint").toString() + "/Bundle";
      myLogger.info(endpoint);
      // TODO: Add headers from the subscription
      String result = "";
      // requestHandler.setURL(endpoint);
      try {
          myLogger.info(notification);
          result = requestHandler.sendPost(endpoint, notification);
      } catch(Exception e) {
          myLogger.info("Error delivering notification");
      }
      return result;
  }

  /**
   * Helper method to get criteria from subscription
   */
  private List<String> getCriteria(JSONWrapper sub) {
      List<String> criteria = new ArrayList<>();
      // put in the default criteria
      criteria.add(sub.get("criteria").getValue().toString());
      if (sub.hasKey("_criteria")) {
          // Add each additional criteria
          for (int i = 0; i < sub.get("_criteria").get("extension").size(); i++) {
              criteria.add(sub.get("_criteria").get("extension").get(i).get("valueString").getValue().toString());
          }
      }
      return criteria;
  }

  /**
   * Helper method to search the server by criteria
   */
  public Bundle searchOnCriteria(String criteria) {
      Bundle results = client.search().byUrl(criteria)
        .returnBundle(Bundle.class)
        .execute();
      return results;
  }

}
