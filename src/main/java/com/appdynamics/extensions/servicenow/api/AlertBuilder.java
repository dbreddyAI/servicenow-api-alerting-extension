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
import java.util.Map;

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


            addRequest("short_description", alert.getShortDescription(), insertElement);
            addRequest("comments", alert.getComments(), insertElement);
            addRequest("impact", alert.getImpact(), insertElement);
            addRequest("priority", alert.getPriority(), insertElement);

            Map<String, String> dynamicProperties = alert.getDynamicProperties();
            if (dynamicProperties != null) {
                for (Map.Entry<String, String> field : dynamicProperties.entrySet()) {
                    if (field.getValue() != null && field.getValue().length() > 0) {
                        addRequest(field.getKey(), field.getValue(), insertElement);
                    }
                }
            }

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

    public static String convertIntoUpdateString(Alert alert, Configuration config, String snowSysId, boolean closeEvent) {

        String serviceNowVersion = config.getServiceNowVersion();
        if ("Calgary".equalsIgnoreCase(serviceNowVersion) || "Dublin".equalsIgnoreCase(serviceNowVersion)) {
            return convertIntoSOAPUpdateString(alert, snowSysId, config, closeEvent);
        } else {
            return convertIntoJsonUpdateString(alert, config, closeEvent);
        }
    }

    private static String convertIntoJsonUpdateString(Alert alert, Configuration config, boolean closeEvent) {

        alert.setImpact(null);
        alert.setPriority(null);
        alert.setShortDescription(null);

        if (closeEvent) {
            alert.setState("Resolved");
            alert.setCloseCode("Closed/Resolved by Caller");
            alert.setCloseNotes(config.getCloseNotesText());
        }

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writeValueAsString(alert);
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Unable to convert the Alert to JSON", e);
        }
    }

    private static String convertIntoSOAPUpdateString(Alert alert, String snowSysId, Configuration config, boolean closeEvent) {
        try {
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = messageFactory.createMessage();
            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
            envelope.addNamespaceDeclaration("inc", "http://www.service-now.com/incident");
            SOAPBody soapBody = envelope.getBody();
            SOAPElement insertElement = soapBody.addChildElement("inc:update");


            addRequest("sys_id", snowSysId, insertElement);
            addRequest("comments", alert.getComments(), insertElement);

            if (closeEvent) {
                addRequest("state", "6", insertElement);
                addRequest("close_code", "Closed/Resolved by Caller", insertElement);
                addRequest("close_notes", config.getCloseNotesText(), insertElement);
            }

            Map<String, String> dynamicProperties = alert.getDynamicProperties();
            if (dynamicProperties != null) {
                for (Map.Entry<String, String> field : dynamicProperties.entrySet()) {
                    if (field.getValue() != null && field.getValue().length() > 0) {
                        addRequest(field.getKey(), field.getValue(), insertElement);
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            soapMessage.writeTo(out);
            return new String(out.toByteArray());
        } catch (SOAPException e) {
            throw new DataParsingException("Unable to convert the Alert to SOAP Message", e);
        } catch (IOException e) {
            throw new DataParsingException("Unable to convert the Alert to SOAP Message", e);
        }
    }
}