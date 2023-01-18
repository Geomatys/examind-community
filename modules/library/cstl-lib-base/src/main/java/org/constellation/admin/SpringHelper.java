/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.admin;

import com.google.common.eventbus.EventBus;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import static org.constellation.admin.SpringHelper.get;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;

/**
 * Pseudo singleton spring helper class.
 * This class is initiated by spring
 *
 */
@Component
public final class SpringHelper {

    private static final Object LOCK = new Object();

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");
    private static SpringHelper INSTANCE;

    @Autowired
    protected ApplicationContext context;

    /**
     * TODO this eventBus should be removed and replaced by the clusterBusiness
     */
    protected EventBus eventBus;

    protected SpringHelper(){
        synchronized(LOCK){
            if(INSTANCE!=null){
                throw new IllegalStateException("Application context has already been set, "
                    + "call closeApplicationContext before starting any new spring context.");
            }
            INSTANCE = this;
        }
    }

    @PostConstruct
    protected void postConstruct(){
        eventBus = context.getBean(EventBus.class);
        LOGGER.info("Spring application context loaded");
    }

    @PreDestroy
    protected void predestroy(){
        synchronized (LOCK){
            if(INSTANCE!=this){
                throw new IllegalStateException("Application context is different, close spring context before opening a new one.");
            }

            //((ConfigurableApplicationContext)get().context).close();
            context = null;
            INSTANCE = null;
            LOGGER.info("Spring application context closed");
        }
    }

    public static SpringHelper get(){
        synchronized (LOCK){
            return INSTANCE;
        }
    }

    public static ApplicationContext getApplicationContext() {
        return get().context;
    }

    public static void injectDependencies(Object object) {
        if (get() != null && get().context != null) {
            get().context.getAutowireCapableBeanFactory().autowireBean(object);
        } else {
            LOGGER.warning("No spring application context available");
        }
    }

    public static <T> T getBean(Class<T> clazz) {
        SpringHelper helper = get();
        if (helper!=null && helper.context != null) {
             return helper.context.getBean(clazz);
        } else {
            LOGGER.warning("No spring application context available");
        }
        return null;
    }

    public static <T> T getBean(String id, Class<T> clazz) {
        SpringHelper helper = get();
        if (helper!=null && helper.context != null) {
             return helper.context.getBean(id, clazz);
        } else {
            LOGGER.warning("No spring application context available");
        }
        return null;
    }

    public static void sendEvent(Object event) {
        SpringHelper helper = get();
        if (helper!=null && helper.eventBus != null) {
            helper.eventBus.post(event);
        } else {
            LOGGER.warning("No event bus available");
        }
    }

    /**
     * Execute TransactionCallback in a transaction.
     * @param callback
     * @return
     */
    public static <T> T executeInTransaction(TransactionCallback<T> callback) {
        if (get() != null) {
            PlatformTransactionManager txManager = get().context.getBean("transactionManager", PlatformTransactionManager.class);
            TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
            return transactionTemplate.execute(callback);
        }  else {
            LOGGER.warning("No spring application context available");
        }
        return null;
    }

    public static <T> T executeInTransaction(TransactionDefinition def, TransactionCallback<T> callback) {
        if (get() != null) {
            PlatformTransactionManager txManager = get().context.getBean("transactionManager", PlatformTransactionManager.class);
            TransactionTemplate transactionTemplate = new TransactionTemplate(txManager, def);
            return transactionTemplate.execute(callback);
        }  else {
            LOGGER.warning("No spring application context available");
        }
        return null;
    }

    /**
     * The method close and dereference the application context.
     * This method should be called only when constellation is shuting down.
     */
    public static void closeApplicationContext(){
        synchronized (LOCK){
            final SpringHelper helper = get();
            if(helper!=null){
                ((ConfigurableApplicationContext)helper.context).close();
                helper.context = null;
            }
            LOGGER.info("Spring application context closed");
        }
    }

}
