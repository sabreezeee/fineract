/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.businessevent.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.event.external.service.ExternalEventService;
import org.apache.fineract.portfolio.businessevent.BusinessEventListener;
import org.apache.fineract.portfolio.businessevent.domain.BusinessEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({ "unchecked", "rawtypes" })
@RequiredArgsConstructor
@Slf4j
public class BusinessEventNotifierServiceImpl implements BusinessEventNotifierService, InitializingBean {

    private final Map<Class, List<BusinessEventListener>> preListeners = new HashMap<>();
    private final Map<Class, List<BusinessEventListener>> postListeners = new HashMap<>();

    private final ExternalEventService externalEventService;
    private final FineractProperties fineractProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (isExternalEventPostingEnabled()) {
            log.info("External event posting is enabled");
        } else {
            log.info("External event posting is disabled");
        }
    }

    @Override
    public void notifyPreBusinessEvent(BusinessEvent<?> businessEvent) {
        List<BusinessEventListener> businessEventListeners = preListeners.get(businessEvent.getClass());
        if (businessEventListeners != null) {
            for (BusinessEventListener eventListener : businessEventListeners) {
                eventListener.onBusinessEvent(businessEvent);
            }
        }
    }

    @Override
    public <T extends BusinessEvent<?>> void addPreBusinessEventListener(Class<T> eventType, BusinessEventListener<T> listener) {
        List<BusinessEventListener> businessEventListeners = preListeners.get(eventType);
        if (businessEventListeners == null) {
            businessEventListeners = new ArrayList<>();
            preListeners.put(eventType, businessEventListeners);
        }
        businessEventListeners.add(listener);
    }

    @Override
    public void notifyPostBusinessEvent(BusinessEvent<?> businessEvent) {
        List<BusinessEventListener> businessEventListeners = postListeners.get(businessEvent.getClass());
        if (businessEventListeners != null) {
            for (BusinessEventListener eventListener : businessEventListeners) {
                eventListener.onBusinessEvent(businessEvent);
            }
        }
        if (isExternalEventPostingEnabled()) {
            // we only want to create external events for operations that were successful, hence the post listener
            externalEventService.postEvent(businessEvent);
        }
    }

    private boolean isExternalEventPostingEnabled() {
        return fineractProperties.getEvents().getExternal().isEnabled();
    }

    @Override
    public <T extends BusinessEvent<?>> void addPostBusinessEventListener(Class<T> eventType, BusinessEventListener<T> listener) {
        List<BusinessEventListener> businessEventListeners = postListeners.get(eventType);
        if (businessEventListeners == null) {
            businessEventListeners = new ArrayList<>();
            postListeners.put(eventType, businessEventListeners);
        }
        businessEventListeners.add(listener);
    }
}
