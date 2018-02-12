/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.servicenow.api;

/**
 * @author Satish Muddam
 */
public class DataParsingException extends RuntimeException {

    public DataParsingException(String msg, Throwable t) {
        super(msg, t);
    }

}
