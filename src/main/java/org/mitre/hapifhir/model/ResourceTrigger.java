package org.mitre.hapifhir.model;

import java.util.List;

import org.hl7.fhir.r4.model.ResourceType;

public class ResourceTrigger {
    public enum MethodCriteria {
        CREATE, UPDATE, DELETE;
    }    

    private ResourceType resourceType;
    private List<MethodCriteria> methodCriteria;

    public ResourceTrigger(ResourceType resourceType, List<MethodCriteria> methodCriteria) {
        this.resourceType = resourceType;
        this.methodCriteria = methodCriteria;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public List<MethodCriteria> getMethodCriteria() {
        return this.methodCriteria;
    }
}
