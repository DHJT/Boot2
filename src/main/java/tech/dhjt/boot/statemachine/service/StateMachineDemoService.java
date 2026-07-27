package tech.dhjt.boot.statemachine.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.dhjt.boot.statemachine.OrderEvents;
import tech.dhjt.boot.statemachine.OrderStates;
import tech.dhjt.boot.statemachine.config.ChoiceJunctionStateMachineConfig;
import tech.dhjt.boot.statemachine.config.DeferStateMachineConfig;
import tech.dhjt.boot.statemachine.config.ForkJoinStateMachineConfig;
import tech.dhjt.boot.statemachine.config.HierarchicalStateMachineConfig;
import tech.dhjt.boot.statemachine.config.OrderStateMachineConfig;

/**
 * 状态机功能演示服务：
 * 1. 订单状态机 + 数据库持久化
 * 2. Choice / Junction
 * 3. Deferred Event
 * 4. 分层状态
 * 5. Fork/Join 并发状态
 */
@Slf4j
@Service
public class StateMachineDemoService {

    private final StateMachineFactory<OrderStates, OrderEvents> orderFactory;
    private final StateMachinePersister<OrderStates, OrderEvents, String> orderPersister;
    private final StateMachineFactory<String, String> choiceJunctionFactory;
    private final StateMachineFactory<String, String> deferFactory;
    private final StateMachineFactory<String, String> hierarchicalFactory;
    private final StateMachineFactory<String, String> forkJoinFactory;

    public StateMachineDemoService(
            @Qualifier(OrderStateMachineConfig.FACTORY_BEAN_NAME) StateMachineFactory<OrderStates, OrderEvents> orderFactory,
            StateMachinePersister<OrderStates, OrderEvents, String> orderPersister,
            @Qualifier(ChoiceJunctionStateMachineConfig.FACTORY_BEAN_NAME) StateMachineFactory<String, String> choiceJunctionFactory,
            @Qualifier(DeferStateMachineConfig.FACTORY_BEAN_NAME) StateMachineFactory<String, String> deferFactory,
            @Qualifier(HierarchicalStateMachineConfig.FACTORY_BEAN_NAME) StateMachineFactory<String, String> hierarchicalFactory,
            @Qualifier(ForkJoinStateMachineConfig.FACTORY_BEAN_NAME) StateMachineFactory<String, String> forkJoinFactory) {
        this.orderFactory = orderFactory;
        this.orderPersister = orderPersister;
        this.choiceJunctionFactory = choiceJunctionFactory;
        this.deferFactory = deferFactory;
        this.hierarchicalFactory = hierarchicalFactory;
        this.forkJoinFactory = forkJoinFactory;
    }

    // ==================== 1. 订单状态机 + 数据库持久化 ====================

    /**
     * 向指定订单的状态机发送事件：先从数据库恢复，再发送事件，最后写回数据库。
     */
    public Map<String, Object> sendOrderEvent(String orderId, OrderEvents event, Integer amount) throws Exception {
        StateMachine<OrderStates, OrderEvents> machine = orderFactory.getStateMachine(orderId);
        // 从数据库恢复历史状态（无记录则保持初始状态）
        orderPersister.restore(machine, orderId);
        OrderStates before = machine.getState().getId();

        MessageBuilder<OrderEvents> builder = MessageBuilder.withPayload(event);
        if (amount != null) {
            builder.setHeader("amount", amount);
        }
        List<StateMachineEventResult<OrderStates, OrderEvents>> results =
                machine.sendEvent(Mono.just(builder.build())).collectList().block();
        boolean accepted = results != null && results.stream()
                .anyMatch(r -> r.getResultType() == StateMachineEventResult.ResultType.ACCEPTED);

        // 写回数据库
        orderPersister.persist(machine, orderId);
        OrderStates after = machine.getState().getId();
        machine.stopReactively().block();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("orderId", orderId);
        resp.put("event", event);
        resp.put("accepted", accepted);
        resp.put("beforeState", before);
        resp.put("afterState", after);
        resp.put("extendedState", machine.getExtendedState().getVariables());
        return resp;
    }

    /**
     * 从数据库恢复并查询订单当前状态。
     */
    public Map<String, Object> getOrderState(String orderId) throws Exception {
        StateMachine<OrderStates, OrderEvents> machine = orderFactory.getStateMachine(orderId);
        orderPersister.restore(machine, orderId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("orderId", orderId);
        resp.put("state", machine.getState().getId());
        resp.put("extendedState", machine.getExtendedState().getVariables());
        machine.stopReactively().block();
        return resp;
    }

    // ==================== 2. Choice / Junction ====================

    public Map<String, Object> choiceDemo(int amount) {
        StateMachine<String, String> machine = choiceJunctionFactory.getStateMachine();
        machine.startReactively().block();
        sendEvent(machine, ChoiceJunctionStateMachineConfig.CHECK_AMOUNT, Map.of("amount", amount));
        String result = machine.getState().getId();
        machine.stopReactively().block();
        return Map.of("feature", "choice", "amount", amount, "resultState", result);
    }

    public Map<String, Object> junctionDemo(int score) {
        StateMachine<String, String> machine = choiceJunctionFactory.getStateMachine();
        machine.startReactively().block();
        sendEvent(machine, ChoiceJunctionStateMachineConfig.CHECK_SCORE, Map.of("score", score));
        String result = machine.getState().getId();
        machine.stopReactively().block();
        return Map.of("feature", "junction", "score", score, "resultState", result);
    }

    // ==================== 3. Deferred Event ====================

    /**
     * 场景：IDLE 收到 TASK 进入 BUSY；BUSY 中再次收到 TASK 被延迟；
     * FINISH 回到 IDLE 后，延迟的 TASK 被自动重放，再次进入 BUSY。
     */
    public Map<String, Object> deferDemo() {
        StateMachine<String, String> machine = deferFactory.getStateMachine();
        List<String> trace = attachTraceListener(machine);
        machine.startReactively().block();

        sendEvent(machine, DeferStateMachineConfig.TASK, Map.of());   // IDLE -> BUSY
        sendEvent(machine, DeferStateMachineConfig.TASK, Map.of());   // BUSY 中被 defer
        trace.add("事件 TASK 在 BUSY 状态被延迟（deferred）");
        sendEvent(machine, DeferStateMachineConfig.FINISH, Map.of()); // BUSY -> IDLE，随后延迟事件重放 -> BUSY

        String finalState = machine.getState().getId();
        machine.stopReactively().block();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("feature", "deferred-event");
        resp.put("trace", trace);
        resp.put("finalState", finalState);
        return resp;
    }

    // ==================== 4. 分层状态 ====================

    public Map<String, Object> hierarchicalDemo() {
        StateMachine<String, String> machine = hierarchicalFactory.getStateMachine();
        List<String> trace = attachTraceListener(machine);
        machine.startReactively().block();

        sendEvent(machine, HierarchicalStateMachineConfig.START, Map.of()); // IDLE -> PROCESSING/VALIDATING
        trace.add("当前激活状态: " + activeIds(machine));
        sendEvent(machine, HierarchicalStateMachineConfig.NEXT, Map.of());  // VALIDATING -> PACKING
        trace.add("当前激活状态: " + activeIds(machine));
        // FINISH 定义在父状态 PROCESSING 上，子状态 PACKING 下同样可触发
        sendEvent(machine, HierarchicalStateMachineConfig.FINISH, Map.of());

        String finalState = machine.getState().getId();
        machine.stopReactively().block();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("feature", "hierarchical-states");
        resp.put("trace", trace);
        resp.put("finalState", finalState);
        return resp;
    }

    // ==================== 5. Fork/Join 并发状态 ====================

    public Map<String, Object> forkJoinDemo() {
        StateMachine<String, String> machine = forkJoinFactory.getStateMachine();
        List<String> trace = attachTraceListener(machine);
        machine.startReactively().block();

        sendEvent(machine, ForkJoinStateMachineConfig.RUN, Map.of()); // fork 进入两个并行 Region
        trace.add("fork 后并行激活状态: " + activeIds(machine));
        sendEvent(machine, ForkJoinStateMachineConfig.FINISH_A, Map.of());
        trace.add("Region A 完成后: " + activeIds(machine));
        sendEvent(machine, ForkJoinStateMachineConfig.FINISH_B, Map.of()); // 两个 Region 均完成 -> join -> DONE
        trace.add("Region B 完成后（join 汇合）: " + activeIds(machine));

        String finalState = machine.getState().getId();
        machine.stopReactively().block();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("feature", "fork-join-regions");
        resp.put("trace", trace);
        resp.put("finalState", finalState);
        return resp;
    }

    // ==================== 工具方法 ====================

    private void sendEvent(StateMachine<String, String> machine, String event, Map<String, Object> headers) {
        Message<String> message = MessageBuilder.withPayload(event).copyHeaders(headers).build();
        machine.sendEvent(Mono.just(message)).collectList().block();
    }

    private String activeIds(StateMachine<String, String> machine) {
        State<String, String> state = machine.getState();
        return state == null ? "N/A" : String.valueOf(state.getIds());
    }

    /**
     * 挂载监听器，记录状态变化轨迹。
     */
    private List<String> attachTraceListener(StateMachine<String, String> machine) {
        List<String> trace = new ArrayList<>();
        machine.addStateListener(new StateMachineListenerAdapter<>() {
            @Override
            public void stateChanged(State<String, String> from, State<String, String> to) {
                trace.add("状态变化: " + (from == null ? "[初始]" : from.getId()) + " -> " + to.getId());
            }

            @Override
            public void eventNotAccepted(Message<String> event) {
                trace.add("事件未被接受: " + event.getPayload());
            }
        });
        return trace;
    }
}
