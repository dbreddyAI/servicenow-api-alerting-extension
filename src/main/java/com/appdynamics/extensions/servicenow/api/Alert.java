package com.appdynamics.extensions.servicenow.api;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Alert {

    @JsonProperty("assignment_group")
    private String assignmentGroup;

    @JsonProperty("assigned_to")
    private String assignedTo;

    @JsonProperty("caller_id")
    private String calledID;

    @JsonProperty("category")
    private String category;

    @JsonProperty("short_description")
    private String shortDescription;

    @JsonProperty("comments")
    private String workNotes;

    @JsonProperty("location")
    private String location;

    @JsonProperty("impact")
    private String impact;

    @JsonProperty("priority")
    private String priority;

    public String getAssignmentGroup() {
        return assignmentGroup;
    }

    public void setAssignmentGroup(String assignmentGroup) {
        this.assignmentGroup = assignmentGroup;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getCalledID() {
        return calledID;
    }

    public void setCalledID(String calledID) {
        this.calledID = calledID;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getWorkNotes() {
        return workNotes;
    }

    public void setWorkNotes(String workNotes) {
        this.workNotes = workNotes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
}