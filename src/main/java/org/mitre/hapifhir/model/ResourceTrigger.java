package org.mitre.hapifhir.model;

import java.util.List;

import org.hl7.fhir.r4.model.ResourceType;

public class ResourceTrigger {
    public enum MethodCriteria {
        CREATE, UPDATE, DELETE;
    }    

    private String currentCriteria;
    private ResourceType resourceType;
    private List<MethodCriteria> methodCriteria;

    /**
     * Create a new ResourceTrigger backbone element. Contains basic properties of the 
     * SubscriptionTopic.resourceTrigger element.
     * 
     * @param resourceType - allowed resource type (resourceTrigger.resourceType)
     * @param methodCriteria - create, update, delete (resourceTrigger.methodCriteria)
     */
    public ResourceTrigger(ResourceType resourceType, List<MethodCriteria> methodCriteria) {
        this(resourceType, methodCriteria, null);
    }

    /**
     * Create a new ResourceTrigger backbone element. Contains basic properties of the 
     * SubscriptionTopic.resourceTrigger element.
     * 
     * @param resourceType - allowed resource type (resourceTrigger.resourceType)
     * @param methodCriteria - create, update, delete (resourceTrigger.methodCriteria)
     * @param currentCriteria - query based trigger rule (resourceTrigger.queryCriteria.current)
     */
    public ResourceTrigger(ResourceType resourceType, List<MethodCriteria> methodCriteria, 
      String currentCriteria) {
        this.resourceType = resourceType;
        this.methodCriteria = methodCriteria;
        this.currentCriteria = currentCriteria;
    }

    public String getCurrentCriteria() {
        return this.currentCriteria;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public List<MethodCriteria> getMethodCriteria() {
        return this.methodCriteria;
    }
}
