package com.appdynamics.extensions.servicenow.common;


import java.util.ArrayList;
import java.util.List;

public class Configuration {

    private String domain;
    private String username;
    private String password;
    private String passwordEncrypted;
    private String encryptionKey;
    private String serviceNowVersion;
    private String proxyUri;
    private String proxyUser;
    private String proxyPassword;

    private String closeNotesText;

    private List<Field> fields = new ArrayList<Field>();


    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getPasswordEncrypted() {
        return passwordEncrypted;
    }

    public void setPasswordEncrypted(String passwordEncrypted) {
        this.passwordEncrypted = passwordEncrypted;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getServiceNowVersion() {
        return serviceNowVersion;
    }

    public void setServiceNowVersion(String serviceNowVersion) {
        this.serviceNowVersion = serviceNowVersion;
    }

    public String getProxyUri() {
        return proxyUri;
    }

    public void setProxyUri(String proxyUri) {
        this.proxyUri = proxyUri;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public String getCloseNotesText() {
        return closeNotesText;
    }

    public void setCloseNotesText(String closeNotesText) {
        this.closeNotesText = closeNotesText;
    }
}
