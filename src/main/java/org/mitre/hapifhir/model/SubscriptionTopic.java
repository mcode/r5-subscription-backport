package org.mitre.hapifhir.model;

import java.util.List;

import org.hl7.fhir.r4.model.Coding;

public class SubscriptionTopic {
    public enum NotificationType {
        HANDSHAKE("handshake", "Handshake"), HEARTBEAT("heartbeat", "Heartbeat"), 
        EVENT_NOTIFICATION("event-notification", "Event Notification"), 
        QUERY_STATUS("query-status", "Query Status");

        private final String code;
        private final String display;
        private final String system = 
          "http://hl7.org/fhir/uv/subscriptions-backport/CodeSystem/backport-notification-type-code-system";

        NotificationType(String code, String display) {
            this.code = code;
            this.display = display;
        }

        public Coding toCoding() {
            return new Coding(system, this.code, this.display);
        }
    }

    private String id;
    private String name;
    private String topicUrl;
    private List<ResourceTrigger> resourceTriggers;

    /**
     * Create a new SubscriptionTopic object. 
     * 
     * @param id - the topic id
     * @param name - the topic name
     * @param topicUrl - the topic canonical url
     * @param resourceTriggers - the list of resourceTriggers
     */
    public SubscriptionTopic(String id, String name, String topicUrl, List<ResourceTrigger> resourceTriggers) {
        this.id = id;
        this.name = name;
        this.topicUrl = topicUrl;
        this.resourceTriggers = resourceTriggers;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getTopicUrl() {
        return this.topicUrl;
    }

    public List<ResourceTrigger> getResourceTriggers() {
        return this.resourceTriggers;
    }

}
