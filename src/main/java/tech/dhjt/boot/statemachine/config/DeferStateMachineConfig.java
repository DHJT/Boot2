package tech.dhjt.boot.statemachine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

/**
 * Deferred Event（延迟事件）演示。
 *
 * <pre>
 * IDLE --TASK--> BUSY
 * BUSY 状态下 TASK 事件被 defer（延迟入队，不丢弃）
 * BUSY --FINISH--> IDLE，回到 IDLE 后延迟的 TASK 立即被重放 --> 再次进入 BUSY
 * </pre>
 */
@Configuration
@EnableStateMachineFactory(name = DeferStateMachineConfig.FACTORY_BEAN_NAME)
public class DeferStateMachineConfig extends StateMachineConfigurerAdapter<String, String> {

    public static final String FACTORY_BEAN_NAME = "deferMachineFactory";

    // 状态
    public static final String IDLE = "IDLE";
    public static final String BUSY = "BUSY";

    // 事件
    public static final String TASK = "TASK";
    public static final String FINISH = "FINISH";

    @Override
    public void configure(StateMachineConfigurationConfigurer<String, String> config) throws Exception {
        config.withConfiguration().machineId("deferMachine").autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
        states
            .withStates()
                .initial(IDLE)
                .state(IDLE)
                // BUSY 状态中延迟 TASK 事件：处理中收到的新任务先排队
                .state(BUSY, TASK);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<String, String> transitions) throws Exception {
        transitions
            .withExternal()
                .source(IDLE).target(BUSY).event(TASK)
                .and()
            .withExternal()
                .source(BUSY).target(IDLE).event(FINISH);
    }
}
