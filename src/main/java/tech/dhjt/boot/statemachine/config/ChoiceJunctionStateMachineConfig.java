package tech.dhjt.boot.statemachine.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;

/**
 * Choice 与 Junction 伪状态演示。
 *
 * <pre>
 * Choice   : WAITING --CHECK_AMOUNT(header:amount)--> AMOUNT_CHOICE --> HIGH / MEDIUM / LOW
 * Junction : WAITING --CHECK_SCORE(header:score)----> SCORE_JUNCTION --> APPROVED / MANUAL_REVIEW / REJECTED
 * </pre>
 *
 * 两者配置方式几乎相同：Choice 语义上是动态条件分支（运行时求值），
 * Junction 是静态条件分支（可作为多条入迁移的汇聚点）。
 */
@Configuration
@EnableStateMachineFactory(name = ChoiceJunctionStateMachineConfig.FACTORY_BEAN_NAME)
public class ChoiceJunctionStateMachineConfig extends StateMachineConfigurerAdapter<String, String> {

    public static final String FACTORY_BEAN_NAME = "choiceJunctionMachineFactory";

    // 状态
    public static final String WAITING = "WAITING";
    public static final String AMOUNT_CHOICE = "AMOUNT_CHOICE";
    public static final String SCORE_JUNCTION = "SCORE_JUNCTION";
    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String LOW = "LOW";
    public static final String APPROVED = "APPROVED";
    public static final String MANUAL_REVIEW = "MANUAL_REVIEW";
    public static final String REJECTED = "REJECTED";

    // 事件
    public static final String CHECK_AMOUNT = "CHECK_AMOUNT";
    public static final String CHECK_SCORE = "CHECK_SCORE";

    @Override
    public void configure(StateMachineConfigurationConfigurer<String, String> config) throws Exception {
        config.withConfiguration().machineId("choiceJunctionMachine").autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
        Set<String> normal = new HashSet<>(Set.of(HIGH, MEDIUM, LOW, APPROVED, MANUAL_REVIEW, REJECTED));
        states
            .withStates()
                .initial(WAITING)
                .choice(AMOUNT_CHOICE)      // choice 伪状态
                .junction(SCORE_JUNCTION)   // junction 伪状态
                .states(normal);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<String, String> transitions) throws Exception {
        transitions
            // ---- Choice：按金额动态路由 ----
            .withExternal()
                .source(WAITING).target(AMOUNT_CHOICE).event(CHECK_AMOUNT)
                .and()
            .withChoice()
                .source(AMOUNT_CHOICE)
                .first(HIGH, headerGtOrEq("amount", 1000))
                .then(MEDIUM, headerGtOrEq("amount", 100))
                .last(LOW)
                .and()
            // ---- Junction：按评分静态路由 ----
            .withExternal()
                .source(WAITING).target(SCORE_JUNCTION).event(CHECK_SCORE)
                .and()
            .withJunction()
                .source(SCORE_JUNCTION)
                .first(APPROVED, headerGtOrEq("score", 90))
                .then(MANUAL_REVIEW, headerGtOrEq("score", 60))
                .last(REJECTED);
    }

    /**
     * 守卫：消息头中的数值 >= 阈值
     */
    private Guard<String, String> headerGtOrEq(String header, int threshold) {
        return ctx -> {
            Number value = ctx.getMessageHeaders().get(header, Number.class);
            return value != null && value.intValue() >= threshold;
        };
    }
}
