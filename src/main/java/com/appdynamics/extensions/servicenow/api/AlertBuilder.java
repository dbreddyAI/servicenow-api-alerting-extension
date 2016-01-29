package com.appdynamics.extensions.servicenow.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class AlertBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String convertIntoJsonString(Alert alert) throws JsonProcessingException {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsString(alert);
    }

    public static Incident convertIntoIncident(String json) throws IOException {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode records = mapper.readTree(json).get("records");

        if (records.isArray()) {
            JsonNode incidentJson = records.get(0);
            Incident incident = mapper.readValue(incidentJson.toString(), Incident.class);
            return incident;
        }
        throw new IOException("Unrecognized response from SNOW");
    }
}