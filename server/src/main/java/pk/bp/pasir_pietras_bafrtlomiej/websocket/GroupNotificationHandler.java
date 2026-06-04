package pk.bp.pasir_pietras_bafrtlomiej.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupNotificationHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GroupNotificationHandler.class);
    private static final String EMAIL_ATTRIBUTE = "userEmail";

    private final Map<String, WebSocketSession> sessionsByEmail = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public GroupNotificationHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String email = (String) session.getAttributes().get(EMAIL_ATTRIBUTE);
        if (email != null) {
            sessionsByEmail.put(email, session);
            log.info("WS connected: {}", email);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = (String) session.getAttributes().get(EMAIL_ATTRIBUTE);
        if (email != null) {
            sessionsByEmail.remove(email, session);
            log.info("WS disconnected: {}", email);
        }
    }

    public void sendToUser(String email, Object payload) {
        WebSocketSession session = sessionsByEmail.get(email);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.warn("WS send failed for {}: {}", email, e.getMessage());
            sessionsByEmail.remove(email, session);
        }
    }
}
