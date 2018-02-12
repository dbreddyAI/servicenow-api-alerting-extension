/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.servicenow.common;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.http.Http4ClientBuilder;
import com.appdynamics.extensions.servicenow.api.Alert;
import com.appdynamics.extensions.servicenow.api.AlertBuilder;
import com.appdynamics.extensions.servicenow.api.DataParsingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpHandler {

    public static final String SOAP_URI = "incident.do?SOAP";
    public static final String REST_URL = "api/now/table/incident";
    public static final String FORWARD_SLASH = "/";

    private CloseableHttpClient httpClient;

    private final Configuration config;
    private static Logger logger = Logger.getLogger(HttpHandler.class);

    public HttpHandler(Configuration config) {
        this.config = config;
        setupHttpClient();
    }

    /**
     * Posts the data to ServiceNow.
     *
     * @param alert
     * @param incidentID
     */
    public boolean postAlert(Alert alert, String incidentID) {

        String payload;
        try {
            payload = AlertBuilder.convertIntoString(alert, config);
            logger.debug("String posting to ServiceNow ::" + payload);
        } catch (DataParsingException e) {
            logger.error("Cannot parse object", e);
            return false;
        }

        try {
            String targetUrl = buildTargetUrl();
            logger.debug("Posting data to ServiceNow at " + targetUrl);

            HttpPost post = new HttpPost(targetUrl);

            if (isOlderVersion()) {
                post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML);
                post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_XML);
            } else {
                post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                post.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            }

            post.setEntity(new StringEntity(payload));

            CloseableHttpResponse response = httpClient.execute(post);

            int status = response.getStatusLine().getStatusCode();
            String responseString = EntityUtils.toString(response.getEntity());

            if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
                logger.info("Data successfully posted to ServiceNow ");
                logger.debug("ServiceNow response " + responseString);

                String sys_id = null;
                if (!isOlderVersion()) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(responseString);
                    JsonNode result = root.path("result");
                    sys_id = result.path("sys_id").asText();
                } else {

                    XmlMapper xmlMapper = new XmlMapper();
                    JsonNode root = xmlMapper.readTree(responseString);
                    JsonNode insertResponse = root.path("Body").path("insertResponse");
                    sys_id = insertResponse.path("sys_id").asText();

                }

                FileSystemStore.INSTANCE.putInStore(incidentID, sys_id);


                return true;
            }
            logger.error("Data post to ServiceNow failed with status " + status + " and error message[" + responseString + "]");
        } catch (IOException e) {
            logger.error("Error while posting data to ServiceNow", e);
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.error("Error while closing the HttpClient", e);
                }
            }
        }
        return false;
    }

    private void setupHttpClient() {

        Map map = createHttpConfigMap();


        //Workaround to ignore the certificate mismatch issue.
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unable to create SSL context", e);
            throw new RuntimeException("Unable to create SSL context", e);
        } catch (KeyManagementException e) {
            logger.error("Unable to create SSL context", e);
            throw new RuntimeException("Unable to create SSL context", e);
        } catch (KeyStoreException e) {
            logger.error("Unable to create SSL context", e);
            throw new RuntimeException("Unable to create SSL context", e);
        }
        HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, (X509HostnameVerifier) hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        HttpClientBuilder builder = Http4ClientBuilder.getBuilder(map);
        builder.setConnectionManager(connMgr);

        builder.setSSLSocketFactory(sslSocketFactory);

        //Keeping only Basic auth
        Registry<AuthSchemeProvider> r = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                .build();

        builder.setDefaultAuthSchemeRegistry(r);

        httpClient = builder.build();
    }

    private Map<String, String> createHttpConfigMap() {
        Map map = new HashMap();

        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        map.put("servers", list);
        HashMap<String, String> server = new HashMap<String, String>();
        server.put("uri", config.getDomain());
        if (config.getUsername() != null && config.getUsername().length() > 0) {
            server.put("username", config.getUsername());
            server.put("password", getPassword());
        }
        list.add(server);

        HashMap<String, String> proxyProps = new HashMap<String, String>();
        map.put("proxy", proxyProps);
        proxyProps.put("uri", config.getProxyUri());
        proxyProps.put("username", config.getProxyUser());
        proxyProps.put("password", config.getProxyPassword());

        return map;
    }

    private String getPassword() {

        String password = config.getPassword();

        Map<String, String> map = new HashMap<String, String>();

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

        String plainPassword = CryptoUtil.getPassword(map);

        return plainPassword;
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

    public boolean updateAlert(Alert alert, String incidentID, String snowSysId, boolean closeEvent) {

        String payload;
        try {
            payload = AlertBuilder.convertIntoUpdateString(alert, config, snowSysId, closeEvent);
            logger.debug("String posting to ServiceNow ::" + payload);
        } catch (DataParsingException e) {
            logger.error("Cannot parse object", e);
            return false;
        }


        try {
            String targetUrl = buildTargetUrlForUpdate(snowSysId);
            logger.debug("Posting data to ServiceNow at " + targetUrl);


            CloseableHttpResponse response = null;
            if (isOlderVersion()) {
                HttpPost post = new HttpPost(targetUrl);
                post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML);
                post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_XML);

                post.setEntity(new StringEntity(payload));

                response = httpClient.execute(post);

            } else {
                HttpPut put = new HttpPut(targetUrl);
                put.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                put.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

                put.setEntity(new StringEntity(payload));

                response = httpClient.execute(put);
            }

            int status = response.getStatusLine().getStatusCode();
            String responseString = EntityUtils.toString(response.getEntity());

            if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
                logger.info("Data successfully posted to ServiceNow ");
                logger.debug("ServiceNow response " + responseString);

                if (closeEvent) {
                    FileSystemStore.INSTANCE.removeFromStore(incidentID);
                }

                return true;
            }
            logger.error("Data post to ServiceNow failed with status " + status + " and error message[" + responseString + "]");
        } catch (IOException e) {
            logger.error("Error while posting data to ServiceNow", e);
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.error("Error while closing the HttpClient", e);
                }
            }
        }
        return false;
    }

    private String buildTargetUrlForUpdate(String snowSysId) {

        StringBuilder sb = new StringBuilder();

        sb.append(config.getDomain());


        if (isOlderVersion()) {
            sb.append(FORWARD_SLASH).append(SOAP_URI);
        } else {
            sb.append(FORWARD_SLASH).append(REST_URL).append("/").append(snowSysId);
        }
        return sb.toString();
    }
}