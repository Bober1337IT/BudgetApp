package pk.bp.pasir_pietras_bafrtlomiej.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import pk.bp.pasir_pietras_bafrtlomiej.websocket.GroupNotificationHandler;
import pk.bp.pasir_pietras_bafrtlomiej.websocket.JwtHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GroupNotificationHandler groupNotificationHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(
            GroupNotificationHandler groupNotificationHandler,
            JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.groupNotificationHandler = groupNotificationHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(groupNotificationHandler, "/ws/group-notifications")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
