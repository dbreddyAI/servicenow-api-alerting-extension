package com.appdynamics.extensions.servicenow.api;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Alert {

    @JsonProperty("short_description")
    private String shortDescription;

    @JsonProperty("comments")
    private String comments;

    @JsonProperty("impact")
    private String impact;

    @JsonProperty("priority")
    private String priority;

    private Map<String, String> dynamicProperties = new HashMap<String, String>();


    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @JsonAnyGetter
    public Map<String, String> getDynamicProperties() {
        return dynamicProperties;
    }

    public void setDynamicProperties(Map<String, String> dynamicProperties) {
        this.dynamicProperties = dynamicProperties;
    }

    public void addDynamicProperties(String name, String value) {
        if (dynamicProperties == null) {
            dynamicProperties = new HashMap<String, String>();
        }

        if (value != null && value.length() > 0) {
            dynamicProperties.put(name, value);
        }
    }
}