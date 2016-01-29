package com.appdynamics.extensions.servicenow.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Satish Muddam
 */
public class Incident {

    @JsonProperty("number")
    private String number;
    @JsonProperty("sys_id")
    private String sysId;


    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getSysId() {
        return sysId;
    }

    public void setSysId(String sysId) {
        this.sysId = sysId;
    }
}
