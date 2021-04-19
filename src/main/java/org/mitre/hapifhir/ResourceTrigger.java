package org.mitre.hapifhir;

import org.hl7.fhir.r4.model.ResourceType;

public class ResourceTrigger {
    public enum MethodCriteria {
        CREATE, UPDATE, DELETE;
    }    

    private ResourceType resourceType;
    private MethodCriteria methodCriteria;

    public ResourceTrigger(ResourceType resourceType, MethodCriteria methodCriteria) {
        this.resourceType = resourceType;
        this.methodCriteria = methodCriteria;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public MethodCriteria getMethodCriteria() {
        return this.methodCriteria;
    }
}
