package com.appdynamics.extensions.servicenow.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AlertBuilder {

    public static String convertIntoJsonString(Alert alert) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(alert);
    }
}
