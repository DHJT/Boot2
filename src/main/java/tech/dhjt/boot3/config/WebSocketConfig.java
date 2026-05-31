package tech.dhjt.boot3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 配置 - 用于实时推送通知
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler(), "/ws/notifications")
                .setAllowedOrigins("*");
    }

    @Bean
    public NotificationWebSocketHandler notificationWebSocketHandler() {
        return new NotificationWebSocketHandler();
    }

    /**
     * WebSocket 处理器 - 管理用户连接并推送通知
     */
    public static class NotificationWebSocketHandler extends TextWebSocketHandler {

        private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
        private static final Map<String, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();
        private static final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 向指定用户推送通知
         */
        public void sendNotification(Long userId, Object notification) {
            String sessionKey = String.valueOf(userId);
            WebSocketSession session = USER_SESSIONS.get(sessionKey);
            if (session != null && session.isOpen()) {
                try {
                    String json = objectMapper.writeValueAsString(notification);
                    session.sendMessage(new TextMessage(json));
                    log.debug("WebSocket 推送通知给用户 {}: {}", userId, json);
                } catch (IOException e) {
                    log.error("WebSocket 推送通知失败，用户 {}: {}", userId, e.getMessage());
                }
            }
        }

        /**
         * 向所有在线用户广播
         */
        public void broadcast(Object notification) {
            USER_SESSIONS.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        String json = objectMapper.writeValueAsString(notification);
                        session.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        log.error("WebSocket 广播失败: {}", e.getMessage());
                    }
                }
            });
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            String userId = extractUserId(session);
            if (userId != null) {
                USER_SESSIONS.put(userId, session);
                log.info("WebSocket 用户 {} 已连接，当前在线数: {}", userId, USER_SESSIONS.size());
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            String userId = extractUserId(session);
            if (userId != null) {
                USER_SESSIONS.remove(userId);
                log.info("WebSocket 用户 {} 已断开，当前在线数: {}", userId, USER_SESSIONS.size());
            }
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            // 客户端可以发送 ping 维持连接
            String payload = message.getPayload();
            if ("ping".equals(payload)) {
                try {
                    session.sendMessage(new TextMessage("pong"));
                } catch (IOException e) {
                    log.error("WebSocket 发送 pong 失败: {}", e.getMessage());
                }
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("WebSocket 传输错误，用户 {}: {}", extractUserId(session), exception.getMessage());
        }

        /**
         * 从查询参数中提取用户ID
         */
        private String extractUserId(WebSocketSession session) {
            String query = session.getUri().getQuery();
            if (query != null && query.startsWith("userId=")) {
                return query.substring(7);
            }
            return null;
        }
    }
}