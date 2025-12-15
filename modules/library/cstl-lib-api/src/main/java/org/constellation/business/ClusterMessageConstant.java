/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.constellation.business;

/**
 *
 * @author guilhem
 */
public class ClusterMessageConstant {

    public static final String KEY_ACTION = "action";
    public static final String KEY_IDENTIFIER = "identifier";

    public static final String SRV_MESSAGE_TYPE_ID = "service";
    
    public static final String PRC_TASK = "task";
    
    public static final String PRC_TASK_STATUS = "taskStatus";
    
    public static final String SRV_SOS_EVENT = "sosEvent";
    
    public static final String SRV_SOS_EVENT_BODY = "sosEventBody";

    public static final String SRV_KEY_TYPE = "type";

    public static final String SRV_VALUE_ACTION_START = "start";
    public static final String SRV_VALUE_ACTION_STOP = "stop";
    public static final String SRV_VALUE_ACTION_REFRESH = "refresh";
    public static final String SRV_VALUE_ACTION_STATUS = "status";
    public static final String SRV_VALUE_ACTION_CLEAR_CACHE = "clearCache";

    public static final String PRV_MESSAGE_TYPE_ID = "provider";

    public static final String PRV_VALUE_ACTION_RELOAD = "reload";
    public static final String PRV_VALUE_ACTION_DELETE = "delete";
    /**
     * Send by DataProviders when a provider is reloaded,changed,deleted...
     * Any structure or content event.
     */
    public static final String PRV_VALUE_ACTION_UPDATED = "updated";

}
