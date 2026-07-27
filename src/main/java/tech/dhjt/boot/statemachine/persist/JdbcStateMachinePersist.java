package tech.dhjt.boot.statemachine.persist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.dhjt.boot.statemachine.OrderEvents;
import tech.dhjt.boot.statemachine.OrderStates;

/**
 * 状态机持久化到数据库（H2，表 state_machine_context）。
 * <p>
 * 通过 JdbcTemplate 保存/恢复 {@link StateMachineContext}：
 * 当前状态存为字符串，扩展状态变量序列化为 JSON。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcStateMachinePersist implements StateMachinePersist<OrderStates, OrderEvents, String> {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void write(StateMachineContext<OrderStates, OrderEvents> context, String machineId) throws Exception {
        String extendedJson = objectMapper.writeValueAsString(context.getExtendedState().getVariables());
        // H2 的 MERGE 语法实现 upsert
        jdbcTemplate.update(
                "MERGE INTO state_machine_context (machine_id, state, extended_state, update_time) "
                        + "KEY (machine_id) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                machineId, context.getState().name(), extendedJson);
        log.info("[persist] 状态机[{}] 已保存, state={}, extended={}", machineId, context.getState(), extendedJson);
    }

    @Override
    public StateMachineContext<OrderStates, OrderEvents> read(String machineId) throws Exception {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT state, extended_state FROM state_machine_context WHERE machine_id = ?", machineId);
        if (rows.isEmpty()) {
            // 无记录：返回 null，状态机将从初始状态开始
            return null;
        }
        Map<String, Object> row = rows.getFirst();
        OrderStates state = OrderStates.valueOf((String) row.get("state"));
        Map<Object, Object> variables = new HashMap<>();
        String extendedJson = (String) row.get("extended_state");
        if (extendedJson != null && !extendedJson.isBlank()) {
            variables = objectMapper.readValue(extendedJson, new TypeReference<>() {});
        }
        log.info("[persist] 状态机[{}] 已恢复, state={}, extended={}", machineId, state, variables);
        return new DefaultStateMachineContext<>(state, null, null, new DefaultExtendedState(variables), null, machineId);
    }
}
