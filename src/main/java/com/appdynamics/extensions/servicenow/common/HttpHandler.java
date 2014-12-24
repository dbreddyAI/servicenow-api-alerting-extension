package com.appdynamics.extensions.servicenow.common;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import org.apache.log4j.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

public class HttpHandler {

    public static final String JSONv2_URI = "incident.do?JSONv2&sysparm_action=insert";
    public static final String REST_URL = "api/now/table/incident";
    public static final String FORWARD_SLASH = "/";

    final Configuration config;
    private static Logger logger = Logger.getLogger(HttpHandler.class);

    public HttpHandler(Configuration config) {
        this.config = config;
    }

    /**
     * Posts the data to ServiceNow.
     *
     * @param data
     * @return
     */
    public Response postAlert(String data) {
        Map<String, String> httpConfigMap = createHttpConfigMap();
        logger.debug("Building the httpClient");
        SimpleHttpClient simpleHttpClient = SimpleHttpClient.builder(httpConfigMap)
                .build();
        String targetUrl = buildTargetUrl();
        logger.debug("Posting data to ServiceNow at " + targetUrl);

        Response response = simpleHttpClient.target(targetUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .post(data);
        logger.debug("HTTP Response status from ServiceNow " + response.getStatus());
        return response;
    }


    private Map<String, String> createHttpConfigMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(TaskInputArgs.USER, config.getUsername());
        map.put(TaskInputArgs.PASSWORD, config.getPassword());

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