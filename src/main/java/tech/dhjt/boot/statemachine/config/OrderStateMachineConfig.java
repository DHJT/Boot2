package tech.dhjt.boot.statemachine.config;

import java.util.EnumSet;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import lombok.extern.slf4j.Slf4j;
import tech.dhjt.boot.statemachine.OrderEvents;
import tech.dhjt.boot.statemachine.OrderStates;

/**
 * 订单状态机配置（配合 JDBC 持久化使用）
 *
 * <pre>
 * PENDING --PAY--> PAID --DELIVER--> DELIVERED --RECEIVE--> COMPLETED
 * PENDING --CANCEL--> CANCELLED
 * </pre>
 */
@Slf4j
@Configuration
@EnableStateMachineFactory(name = OrderStateMachineConfig.FACTORY_BEAN_NAME)
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderStates, OrderEvents> {

    public static final String FACTORY_BEAN_NAME = "orderMachineFactory";
    public static final String MACHINE_ID = "orderMachine";

    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderStates, OrderEvents> config) throws Exception {
        config
            .withConfiguration()
                .machineId(MACHINE_ID)
                // 由持久化恢复流程手动启动，不自动启动
                .autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<OrderStates, OrderEvents> states) throws Exception {
        states
            .withStates()
                .initial(OrderStates.PENDING)
                .states(EnumSet.allOf(OrderStates.class))
                .end(OrderStates.COMPLETED)
                .end(OrderStates.CANCELLED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStates, OrderEvents> transitions) throws Exception {
        transitions
            .withExternal()
                .source(OrderStates.PENDING).target(OrderStates.PAID)
                .event(OrderEvents.PAY)
                // 演示扩展状态变量的持久化：把支付金额记录到 ExtendedState
                .action(ctx -> {
                    Object amount = ctx.getMessageHeaders().get("amount");
                    if (amount != null) {
                        ctx.getExtendedState().getVariables().put("payAmount", amount);
                    }
                    log.info("[orderMachine] 支付成功, amount={}", amount);
                })
                .and()
            .withExternal()
                .source(OrderStates.PAID).target(OrderStates.DELIVERED)
                .event(OrderEvents.DELIVER)
                .and()
            .withExternal()
                .source(OrderStates.DELIVERED).target(OrderStates.COMPLETED)
                .event(OrderEvents.RECEIVE)
                .and()
            .withExternal()
                .source(OrderStates.PENDING).target(OrderStates.CANCELLED)
                .event(OrderEvents.CANCEL);
    }
}
