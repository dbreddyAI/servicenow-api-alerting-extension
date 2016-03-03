package com.appdynamics.extensions.servicenow.api;

import com.appdynamics.extensions.servicenow.common.Configuration;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AlertBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String convertIntoJsonString(Alert alert) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writeValueAsString(alert);
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Unable to convert the Alert to JSON", e);
        }
    }

    public static String convertIntoString(Alert alert, Configuration config) {
        String serviceNowVersion = config.getServiceNowVersion();
        if ("Calgary".equalsIgnoreCase(serviceNowVersion) || "Dublin".equalsIgnoreCase(serviceNowVersion)) {
            return convertIntoSOAPString(alert);
        } else {
            return convertIntoJsonString(alert);
        }
    }

    private static String convertIntoSOAPString(Alert alert) {

        try {
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = messageFactory.createMessage();
            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            envelope.addNamespaceDeclaration("tns", "http://www.service-now.com/incident");
            envelope.addNamespaceDeclaration("m", "http://www.service-now.com");
            envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
            SOAPBody soapBody = envelope.getBody();
            SOAPElement insertElement = soapBody.addChildElement("insert", "", "http://www.service-now.com");


            addRequest("assignment_group", alert.getAssignmentGroup(), insertElement);
            addRequest("assigned_to", alert.getAssignedTo(), insertElement);
            addRequest("caller_id", alert.getCalledID(), insertElement);
            addRequest("category", alert.getCategory(), insertElement);
            addRequest("short_description", alert.getShortDescription(), insertElement);
            addRequest("comments", alert.getComments(), insertElement);
            addRequest("location", alert.getLocation(), insertElement);
            addRequest("impact", alert.getImpact(), insertElement);
            addRequest("priority", alert.getPriority(), insertElement);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            soapMessage.writeTo(out);
            return new String(out.toByteArray());
        } catch (SOAPException e) {
            throw new DataParsingException("Unable to convert the Alert to SOAP Message", e);
        } catch (IOException e) {
            throw new DataParsingException("Unable to convert the Alert to SOAP Message", e);
        }
    }

    private static void addRequest(String name, String value, SOAPElement element) throws SOAPException {

        if (value != null && value.length() > 0) {
            SOAPElement status = element.addChildElement(name);
            status.addAttribute(new QName("xsi:type"), "xsd:string");
            status.addTextNode(value);
        }

    }
}