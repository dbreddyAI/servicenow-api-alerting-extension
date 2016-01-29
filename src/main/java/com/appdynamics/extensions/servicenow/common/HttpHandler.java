package com.appdynamics.extensions.servicenow.common;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.servicenow.api.Alert;
import com.appdynamics.extensions.servicenow.api.AlertBuilder;
import com.appdynamics.extensions.servicenow.api.Incident;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.log4j.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class HttpHandler {

    public static final String JSONv2_URI = "incident.do?JSONv2&sysparm_action=insert";
    public static final String JOURNAL_URI = "sys_journal_field.do?JSONv2&sysparm_action=insert";
    public static final String REST_URL = "api/now/table/incident";
    public static final String FORWARD_SLASH = "/";

    private static final String JOURNAL_FIELD_COMMENT = "{\n" +
            "    \"element\": \"comments\"\n" +
            "    \"element_id\": \"${SYS_ID}\"\n" +
            "    \"name\": \"task\"\n" +
            "    \"value\": \"${Comments}\"\n" +
            "}";

    final Configuration config;
    private static Logger logger = Logger.getLogger(HttpHandler.class);

    public HttpHandler(Configuration config) {
        this.config = config;
    }

    /**
     * Posts the data to ServiceNow.
     *
     * @param alert
     * @return
     */
    public boolean postAlert(Alert alert) {

        String json;
        try {
            json = AlertBuilder.convertIntoJsonString(alert);
            logger.debug("Json posting to ServiceNow ::" + json);
        } catch (JsonProcessingException e) {
            logger.error("Cannot serialize object into Json.", e);
            return false;
        }

        Map<String, String> httpConfigMap = createHttpConfigMap();
        logger.debug("Building the httpClient");
        SimpleHttpClient simpleHttpClient = null;

        try {
            simpleHttpClient = SimpleHttpClient.builder(httpConfigMap)
                    .build();
            String targetUrl = buildTargetUrl();
            logger.debug("Posting data to ServiceNow at " + targetUrl);

            Response response = simpleHttpClient.target(targetUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                    .post(json);
            int status = response.getStatus();
            if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {

                logger.info("Data successfully posted to ServiceNow");

                if (targetUrl.contains("JSONv2")) {
                    logger.info("Trying to post comments to JSONv2 supported versions of SNOW");
                    String string = response.string();
                    try {
                        Incident incident = AlertBuilder.convertIntoIncident(string);
                        logger.debug("Posting comments to incident with id " + incident.getNumber());
                        postCommentsToOlderVersions(simpleHttpClient, incident.getSysId(), alert.getComments());
                    } catch (IOException e) {
                        logger.error("Unable to read the incident response, so not able to post comments to the incident", e);
                    }
                }

                return true;
            }
            logger.error("Data post to ServiceNow failed with status " + status + " and error message[" + response.string() + "]");
        } finally {
            if (simpleHttpClient != null) {
                simpleHttpClient.close();
            }
        }
        return false;
    }

    private void postCommentsToOlderVersions(SimpleHttpClient simpleHttpClient, String sysId, String comments) {
        StringBuilder sb = new StringBuilder();

        sb.append(config.getDomain());

        sb.append(FORWARD_SLASH).append(JOURNAL_URI);

        String commentData = JOURNAL_FIELD_COMMENT.replace("${SYS_ID}", sysId);
        commentData = commentData.replace("${Comments}", comments);


        Response response = simpleHttpClient.target(sb.toString())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .post(commentData);
        int status = response.getStatus();

        if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
            logger.info("Comments posted successfully");

        } else {
            logger.error("Posting comments failed with status " + status + " and error message[" + response.string() + "]");
        }
    }


    private Map<String, String> createHttpConfigMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(TaskInputArgs.USER, config.getUsername());
        String password = config.getPassword();

        if (password != null) {
            logger.debug("Using provided password");
            map.put(TaskInputArgs.PASSWORD, password);
        }

        String passwordEncrypted = config.getPasswordEncrypted();
        if (passwordEncrypted != null) {
            logger.debug("Using provided passwordEncrypted");
            map.put(TaskInputArgs.PASSWORD_ENCRYPTED, passwordEncrypted);
            map.put(TaskInputArgs.ENCRYPTION_KEY, config.getEncryptionKey());
        }

        map.put(TaskInputArgs.PROXY_HOST, config.getProxyHost());
        map.put(TaskInputArgs.PROXY_PORT, config.getProxyPort());
        map.put(TaskInputArgs.PROXY_USER, config.getProxyUser());
        map.put(TaskInputArgs.PROXY_PASSWORD, config.getProxyPassword());

        return map;
    }

    private String buildTargetUrl() {
        StringBuilder sb = new StringBuilder();

        sb.append(config.getDomain());

        String serviceNowVersion = config.getServiceNowVersion();
        if ("Calgary".equalsIgnoreCase(serviceNowVersion) || "Dublin".equalsIgnoreCase(serviceNowVersion)) {
            sb.append(FORWARD_SLASH).append(JSONv2_URI);
        } else {
            sb.append(FORWARD_SLASH).append(REST_URL);
        }
        return sb.toString();
    }
}