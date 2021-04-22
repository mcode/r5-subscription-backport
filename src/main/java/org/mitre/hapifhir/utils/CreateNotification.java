package org.mitre.hapifhir.utils;

import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.BaseReference;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Subscription;
import org.mitre.hapifhir.model.SubscriptionTopic.NotificationType;

public class CreateNotification {

    /**
     * Create an R5 Backport Notification.
     * 
     * @param subscription - the subscription to notify
     * @param resources - list of resources to include (empty or null for empty notification)
     * @param baseUrl - the server base url
     * @param topicUrl - the canonical url of the topic
     * @param notificationType - R5 Subscription Notification Type Value Set
     * @return the notification Bundle
     */
    public static Bundle createResourceNotification(Subscription subscription, List<Resource> resources,
      String baseUrl, String topicUrl, NotificationType notificationType) {
        Meta meta = new Meta();
        meta.addProfile("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-subscription-notification");

        String subscriptionFullUrl = baseUrl + "/Subscription/" + subscription.getId();
        Parameters parameters = new Parameters();
        BaseReference subscriptionReference = new Reference(subscriptionFullUrl);
        parameters.addParameter("subscription", subscriptionReference);
        parameters.addParameter("topic", new CanonicalType(topicUrl));
        parameters.addParameter("status", new CodeType(subscription.getStatus().toCode()));
        parameters.addParameter("type", notificationType.toCoding().getCodeElement());

        BundleEntryComponent subscriptionStatusComponent = new BundleEntryComponent();
        subscriptionStatusComponent.setResource(parameters);
        subscriptionStatusComponent.setFullUrl(subscriptionFullUrl);

        Bundle notificationBundle = new Bundle();
        notificationBundle.setType(BundleType.DOCUMENT);
        notificationBundle.setMeta(meta);
        notificationBundle.setTimestamp(new Date());
        notificationBundle.addEntry(subscriptionStatusComponent);

        // TODO: support id-only notifications
        if (resources != null && !resources.isEmpty()) {
            for (Resource r : resources) {
                BundleEntryComponent bec = new BundleEntryComponent();
                bec.setResource(r);
                bec.setFullUrl(baseUrl + "/" + r.fhirType() + "/" + r.getId());
                notificationBundle.addEntry(bec);
            }
        }

        return notificationBundle;
    }
}

