# R5 Subscription Backport Library

This project is a library to add the [R5 Subscription Backport IG](https://build.fhir.org/ig/HL7/fhir-subscription-backport-ig/index.html) functionality to HAPI-based FHIR servers. Due to the many different ways HAPI servers can be setup, there is some configuration required.

# Installation

This project can be added to an existing Maven-based project, add this dependency to `pom.xml`:

```xml
<dependency>
  <groupId>org.mitre.hapifhir</groupId>
  <artifactId>r5-subscription-backport</artifactId>
  <version>0.0.1</version>
</dependency>
```

Or for a Gradle-based project, add this to `build.gradle`:

```
compile 'org.mitre.hapifhir:r5-subscription-backport:0.0.1'

```

# Usage

There are two interceptors included in this library to support the backport ig, `SubscriptionInterceptor` and `TopicListInterceptor`. These must be registered in the HAPI server and include a list of all `SubscriptionTopic`s supported. Furthermore, since this will take the place of the HAPI Subscription capabilites you will need to disable that.

For example, using a JPA Starter HAPI FHIR Server:

```java

import org.mitre.hapifhir.model.SubscriptionTopic;
import org.mitre.hapifhir.model.ResourceTrigger;
import org.mitre.hapifhir.client.IServerClient;
import org.mitre.hapifhir.client.NoAuthServerClient;
import org.mitre.hapifhir.SubscriptionInterceptor;
import org.mitre.hapifhir.TopicListInterceptor;

...

public class JpaRestfulServer extends RestfulServer {


  protected void initialize() {
    ...
    List<SubscriptionTopic> subscriptionTopics = new ArrayList<>();
    ResourceTrigger resourceTrigger = new ResourceTrigger(resourceType, methodCriteria);
    SubscriptionTopic topic = new SubscriptionTopic(id, name, canonicalUrl, Collections.singletonList(resourceTrigger));
    subscriptionTopics.add(topic);
    ...
    IGenericClient client = this.getFhirContext().newRestfulGenericClient(HapiProperties.getServerAddress());
    IServerClient serverClient = new NoAuthServerClient(client);
    SubscriptionInterceptor subscriptionInterceptor =
      new SubscriptionInterceptor(HapiProperties.getAuthServerAddress(), this.getFhirContext(), serverClient, subscriptionTopics);
    this.registerInterceptor(subscriptionInterceptor);
    ...
    TopicListInterceptor topicListInterceptor =
      new TopicListInterceptor(this.getFhirContext(), subscriptionTopics);
    this.registerInterceptor(topicListInterceptor);
    ...
  }
}
```

And in `hapi.properties`

```
# Disable REST Hook Subscription Channel
subscription.resthook.enabled=false
```

# Development

To install the current working version to your local Maven repo, run

```
./gradlew publishToMavenLocal
```

# License

Copyright 2021 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
