package tech.dhjt.boot.statemachine.persist;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;

import tech.dhjt.boot.statemachine.OrderEvents;
import tech.dhjt.boot.statemachine.OrderStates;

/**
 * 状态机持久化器配置
 */
@Configuration
public class StateMachinePersistConfig {

    @Bean
    public StateMachinePersister<OrderStates, OrderEvents, String> orderStateMachinePersister(
            JdbcStateMachinePersist jdbcStateMachinePersist) {
        return new DefaultStateMachinePersister<>(jdbcStateMachinePersist);
    }
}
