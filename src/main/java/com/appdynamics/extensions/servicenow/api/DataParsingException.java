package com.appdynamics.extensions.servicenow.api;

/**
 * @author Satish Muddam
 */
public class DataParsingException extends RuntimeException {

    public DataParsingException(String msg, Throwable t) {
        super(msg, t);
    }

}
