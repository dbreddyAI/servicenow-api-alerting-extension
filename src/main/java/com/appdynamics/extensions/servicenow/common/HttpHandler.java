package com.appdynamics.extensions.servicenow.common;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.WebTarget;
import com.appdynamics.extensions.servicenow.api.Alert;
import com.appdynamics.extensions.servicenow.api.AlertBuilder;
import com.appdynamics.extensions.servicenow.api.DataParsingException;
import org.apache.log4j.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class HttpHandler {

    public static final String SOAP_URI = "incident.do?SOAP";
    public static final String REST_URL = "api/now/table/incident";
    public static final String FORWARD_SLASH = "/";

    private final Configuration config;
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

        String payload;
        try {
            payload = AlertBuilder.convertIntoString(alert, config);
            logger.debug("String posting to ServiceNow ::" + payload);
        } catch (DataParsingException e) {
            logger.error("Cannot parse object", e);
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

            WebTarget target = simpleHttpClient.target(targetUrl);
            
            if (isOlderVersion()) {
                target.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)
                        .header(HttpHeaders.ACCEPT, MediaType.TEXT_XML);
            } else {
                target.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            }

            Response response = target.post(payload);
            int status = response.getStatus();
            if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {

                logger.info("Data successfully posted to ServiceNow ");
                logger.debug("ServiceNow response " + response.string());
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


        if (isOlderVersion()) {
            sb.append(FORWARD_SLASH).append(SOAP_URI);
        } else {
            sb.append(FORWARD_SLASH).append(REST_URL);
        }
        return sb.toString();
    }

    private boolean isOlderVersion() {
        String serviceNowVersion = config.getServiceNowVersion();
        return "Calgary".equalsIgnoreCase(serviceNowVersion) || "Dublin".equalsIgnoreCase(serviceNowVersion);
    }
}