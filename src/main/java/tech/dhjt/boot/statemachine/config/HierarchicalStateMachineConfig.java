package tech.dhjt.boot.statemachine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

/**
 * 分层状态（Hierarchical / Substates）演示。
 *
 * <pre>
 * IDLE --START--> PROCESSING（父状态）
 *                   ├─ VALIDATING（子状态，初始）--NEXT--> PACKING（子状态）
 * PROCESSING --FINISH--> DONE     （定义在父状态上：任意子状态下均可触发）
 * PROCESSING --ABORT---> IDLE     （同上，体现父状态转移对子状态的“继承”）
 * </pre>
 */
@Configuration
@EnableStateMachineFactory(name = HierarchicalStateMachineConfig.FACTORY_BEAN_NAME)
public class HierarchicalStateMachineConfig extends StateMachineConfigurerAdapter<String, String> {

    public static final String FACTORY_BEAN_NAME = "hierarchicalMachineFactory";

    // 状态
    public static final String IDLE = "IDLE";
    public static final String PROCESSING = "PROCESSING";
    public static final String VALIDATING = "VALIDATING";
    public static final String PACKING = "PACKING";
    public static final String DONE = "DONE";

    // 事件
    public static final String START = "START";
    public static final String NEXT = "NEXT";
    public static final String FINISH = "FINISH";
    public static final String ABORT = "ABORT";

    @Override
    public void configure(StateMachineConfigurationConfigurer<String, String> config) throws Exception {
        config.withConfiguration().machineId("hierarchicalMachine").autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
        states
            .withStates()
                .initial(IDLE)
                .state(PROCESSING)
                .state(DONE)
                .and()
            // PROCESSING 的子状态
            .withStates()
                .parent(PROCESSING)
                .initial(VALIDATING)
                .state(PACKING);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<String, String> transitions) throws Exception {
        transitions
            .withExternal()
                .source(IDLE).target(PROCESSING).event(START)
                .and()
            .withExternal()
                .source(VALIDATING).target(PACKING).event(NEXT)
                .and()
            // 定义在父状态上的转移，任何子状态均可响应
            .withExternal()
                .source(PROCESSING).target(DONE).event(FINISH)
                .and()
            .withExternal()
                .source(PROCESSING).target(IDLE).event(ABORT);
    }
}
