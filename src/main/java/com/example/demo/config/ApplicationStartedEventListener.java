package com.example.demo.config;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 应用监听器事件执行先后顺序如下：
    ApplicationStartingEvent
    ApplicationEnvironmentPreparedEvent
    ApplicationPreparedEvent
    ApplicationStartedEvent
    ApplicationReadyEvent
    ApplicationFailedEvent
 * @description
 * @author DHJT 2021-09-05 16:05:40
 *
 */
@Component
public class ApplicationStartedEventListener implements ApplicationListener<ApplicationEvent> {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // TODO Auto-generated method stub
//        event.
    }

}
