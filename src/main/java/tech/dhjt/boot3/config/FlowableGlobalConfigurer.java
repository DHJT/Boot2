package tech.dhjt.boot3.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import tech.dhjt.boot3.listener.GlobalProcessEventListener;

/**
 * Flowable 全局监听器配置 — 注册全局流程事件监听器
 *
 * 将 GlobalProcessEventListener 注册到 Flowable 引擎，
 * 使其可以监听所有流程的全局事件（启动、结束、取消、任务创建、任务完成等）
 */
@RequiredArgsConstructor
@Configuration
public class FlowableGlobalConfigurer {

    private static final Logger log = LoggerFactory.getLogger(FlowableGlobalConfigurer.class);

    private final ProcessEngine processEngine;
    private final GlobalProcessEventListener globalProcessEventListener;

    @PostConstruct
    public void registerGlobalListeners() {
        ProcessEngineConfigurationImpl config =
                (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

        // 注册全局事件监听器
        config.getEventDispatcher().addEventListener(globalProcessEventListener);

        log.info("Flowable 全局事件监听器已注册: {}", globalProcessEventListener.getClass().getSimpleName());
    }
}