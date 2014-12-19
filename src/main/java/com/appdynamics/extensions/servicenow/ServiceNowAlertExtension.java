package com.appdynamics.extensions.servicenow;


import com.appdynamics.extensions.alerts.customevents.EvaluationEntity;
import com.appdynamics.extensions.alerts.customevents.Event;
import com.appdynamics.extensions.alerts.customevents.EventBuilder;
import com.appdynamics.extensions.alerts.customevents.HealthRuleViolationEvent;
import com.appdynamics.extensions.alerts.customevents.TriggerCondition;
import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.servicenow.api.Alert;
import com.appdynamics.extensions.servicenow.api.AlertBuilder;
import com.appdynamics.extensions.servicenow.common.Configuration;
import com.appdynamics.extensions.servicenow.common.HttpHandler;
import com.appdynamics.extensions.yml.YmlReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.List;

public class ServiceNowAlertExtension {

    private static Logger logger = Logger.getLogger(ServiceNowAlertExtension.class);
    private static final String CONFIG_FILENAME = "." + File.separator + "conf" + File.separator + "config.yaml";
    private Configuration config;
    private static final String NEW_LINE = "\n";

    final EventBuilder eventBuilder = new EventBuilder();

    public static void main(String[] args) {
        Configuration config = YmlReader.readFromFile(CONFIG_FILENAME, Configuration.class);
        ServiceNowAlertExtension serviceNowAlert = new ServiceNowAlertExtension(config);
        serviceNowAlert.processAnEvent(args);
    }

    public ServiceNowAlertExtension(Configuration config) {
        String msg = "ServiceNowAlert Version [" + getImplementationTitle() + "]";
        logger.info(msg);
        System.out.println(msg);
        this.config = config;
    }


    public int processAnEvent(String[] args) {
        Event event = eventBuilder.build(args);
        if (event != null) {
            HealthRuleViolationEvent violationEvent = (HealthRuleViolationEvent) event;

            String summery = buildSummery(violationEvent);

            Alert alert = new Alert();
            alert.setAssignedTo(config.getAssignedTo());
            alert.setAssignmentGroup(config.getAssignmentGroup());
            alert.setCalledID(config.getCallerId());
            alert.setCategory(config.getCategory());
            alert.setImpact(getImpact(violationEvent));
            alert.setLocation(config.getLocation());
            alert.setPriority(violationEvent.getPriority());
            alert.setShortDescription("Policy Violated - View more details inside");
            alert.setWorkNotes(summery);
            try {
                HttpHandler handler = new HttpHandler(config);
                String json = AlertBuilder.convertIntoJsonString(alert);
                logger.debug("Json posted to ServiceNow ::" + json);
                Response response = handler.postAlert(json);
                int status = response.getStatus();
                if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
                    logger.info("Data successfully posted to ServiceNow");
                    return 1;
                }
                logger.error("Data post failed");
            } catch (JsonProcessingException e) {
                logger.error("Cannot serialized object into Json." + e);
            }
        }
        return -1;
    }

    private String getImpact(HealthRuleViolationEvent violationEvent) {

        String severity = violationEvent.getSeverity();
        String severityInt = null;
        if("ERROR".equals(severity)) {
            severityInt = "1";
        } else if("WARN".equals(severity)) {
            severityInt = "2";
        } else {
            severityInt = "3";
        }
        return severityInt;
    }

    private String buildSummery(HealthRuleViolationEvent violationEvent) {
        StringBuilder summery = new StringBuilder();
        summery.append("Application Name:").append(violationEvent.getAppName()).append(NEW_LINE);
        summery.append("Policy Violation Alert Time:").append(violationEvent.getPvnAlertTime()).append(NEW_LINE);
        summery.append("Severity:").append(violationEvent.getSeverity()).append(NEW_LINE);
        summery.append("Name of Violated Policy:").append(violationEvent.getHealthRuleName()).append(NEW_LINE);
        summery.append("Affected Entity Type:").append(violationEvent.getAffectedEntityType()).append(NEW_LINE);
        summery.append("Name of Affected Entity:").append(violationEvent.getAffectedEntityName()).append(NEW_LINE);

        List<EvaluationEntity> evaluationEntities = violationEvent.getEvaluationEntity();
        for (int i = 0; i < evaluationEntities.size(); i++) {
            EvaluationEntity evaluationEntity = evaluationEntities.get(i);
            summery.append("EVALUATION ENTITY #").append(i+1).append(":").append(NEW_LINE);
            summery.append("Evaluation Entity:").append(evaluationEntity.getType()).append(NEW_LINE);
            summery.append("Evaluation Entity Name:").append(evaluationEntity.getName()).append(NEW_LINE);

            List<TriggerCondition> triggeredConditions = evaluationEntity.getTriggeredConditions();
            for (int j = 0; j < triggeredConditions.size(); j++) {
                TriggerCondition triggerCondition = triggeredConditions.get(j);
                summery.append("Triggered Condition #").append(j+1).append(":").append(NEW_LINE).append(NEW_LINE);
                summery.append("Scope Type:").append(triggerCondition.getScopeType()).append(NEW_LINE);
                summery.append("Scope Name:").append(triggerCondition.getScopeName()).append(NEW_LINE);

                if (triggerCondition.getConditionUnitType() != null && triggerCondition.getConditionUnitType().toUpperCase().startsWith("BASELINE")) {
                    summery.append("Is Default Baseline?").append(triggerCondition.isUseDefaultBaseline() ? "true" : "false").append(NEW_LINE);
                    if (!triggerCondition.isUseDefaultBaseline()) {
                        summery.append("Baseline Name:").append(triggerCondition.getBaselineName()).append(NEW_LINE);
                    }
                }
                summery.append(triggerCondition.getConditionName()).append(triggerCondition.getOperator()).append(triggerCondition.getThresholdValue()).append(NEW_LINE);
                summery.append("Violation Value:").append(triggerCondition.getObservedValue()).append(NEW_LINE).append(NEW_LINE);
                summery.append("Incident URL:").append(violationEvent.getIncidentUrl());
            }
        }
        return summery.toString();
    }

    private String getImplementationTitle() {
        return this.getClass().getPackage().getImplementationTitle();
    }

}
