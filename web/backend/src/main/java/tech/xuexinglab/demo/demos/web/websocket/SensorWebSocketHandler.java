package tech.xuexinglab.demo.demos.web.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 处理器
 * 维护所有前端连接，提供数据广播功能
 */
public class SensorWebSocketHandler extends TextWebSocketHandler {

        private static final Logger log = LoggerFactory.getLogger(SensorWebSocketHandler.class);

        // 线程安全的会话集合
        private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

        @Override
        public void afterConnectionEstablished(@NonNull WebSocketSession session) {
                sessions.add(session);
                log.info("WebSocket 连接建立: {}, 当前在线: {}", session.getId(), sessions.size());
        }

        @Override
        public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
                sessions.remove(session);
                log.info("WebSocket 连接关闭: {}, 当前在线: {}", session.getId(), sessions.size());
        }

        @Override
        public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
                log.error("WebSocket 传输错误: {}", session.getId(), exception);
                sessions.remove(session);
        }

        /**
         * 向所有已连接的前端推送数据
         * 供 Service 层调用
         */
        public void broadcast(@NonNull String message) {
                TextMessage textMessage = new TextMessage((CharSequence) message);
                for (WebSocketSession session : sessions) {
                        if (session.isOpen()) {
                                try {
                                        session.sendMessage(textMessage);
                                } catch (IOException e) {
                                        log.error("WebSocket 推送失败: {}", session.getId(), e);
                                }
                        }
                }
        }

        /**
         * 获取当前在线连接数
         */
        public int getOnlineCount() {
                return sessions.size();
        }
}
