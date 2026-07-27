package tech.dhjt.boot.statemachine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

/**
 * 并发状态（Orthogonal Regions + Fork/Join）演示。
 *
 * <pre>
 * READY --RUN--> FORK ==> TASKS（父状态，含两个并行 Region）
 *                          ├─ Region A: A1 --FINISH_A--> A2
 *                          └─ Region B: B1 --FINISH_B--> B2
 * A2 与 B2 均到达后 ==> JOIN --> DONE
 * </pre>
 */
@Configuration
@EnableStateMachineFactory(name = ForkJoinStateMachineConfig.FACTORY_BEAN_NAME)
public class ForkJoinStateMachineConfig extends StateMachineConfigurerAdapter<String, String> {

    public static final String FACTORY_BEAN_NAME = "forkJoinMachineFactory";

    // 状态
    public static final String READY = "READY";
    public static final String FORK = "FORK";
    public static final String TASKS = "TASKS";
    public static final String A1 = "A1";
    public static final String A2 = "A2";
    public static final String B1 = "B1";
    public static final String B2 = "B2";
    public static final String JOIN = "JOIN";
    public static final String DONE = "DONE";

    // 事件
    public static final String RUN = "RUN";
    public static final String FINISH_A = "FINISH_A";
    public static final String FINISH_B = "FINISH_B";

    @Override
    public void configure(StateMachineConfigurationConfigurer<String, String> config) throws Exception {
        config.withConfiguration().machineId("forkJoinMachine").autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
        states
            .withStates()
                .initial(READY)
                .fork(FORK)
                .state(TASKS)
                .join(JOIN)
                .state(DONE)
                .and()
            // Region A
            .withStates()
                .parent(TASKS)
                .initial(A1)
                .state(A2)
                .and()
            // Region B
            .withStates()
                .parent(TASKS)
                .initial(B1)
                .state(B2);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<String, String> transitions) throws Exception {
        transitions
            .withExternal()
                .source(READY).target(FORK).event(RUN)
                .and()
            // fork：一次进入两个并行 Region
            .withFork()
                .source(FORK)
                .target(A1)
                .target(B1)
                .and()
            .withExternal()
                .source(A1).target(A2).event(FINISH_A)
                .and()
            .withExternal()
                .source(B1).target(B2).event(FINISH_B)
                .and()
            // join：两个 Region 都到达末端后汇合
            .withJoin()
                .source(A2)
                .source(B2)
                .target(JOIN)
                .and()
            .withExternal()
                .source(JOIN).target(DONE);
    }
}
