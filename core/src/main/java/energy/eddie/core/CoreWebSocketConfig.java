package energy.eddie.core;

import energy.eddie.core.services.PermissionService;
import energy.eddie.core.web.ConnectionStatusWebsocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CoreWebSocketConfig implements WebSocketConfigurer {
    private final PermissionService permissionService;

    public CoreWebSocketConfig(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(connectionStatusWebsocketHandler(), "/api/connection-status-messages");
    }

    @Bean
    public ConnectionStatusWebsocketHandler connectionStatusWebsocketHandler() {
        return new ConnectionStatusWebsocketHandler(permissionService);
    }
}
