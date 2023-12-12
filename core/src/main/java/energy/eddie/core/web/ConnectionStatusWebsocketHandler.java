package energy.eddie.core.web;

import energy.eddie.core.services.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.adapter.JdkFlowAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionStatusWebsocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusWebsocketHandler.class);
    private final List<WebSocketSession> webSocketSessions = new ArrayList<>();

    public ConnectionStatusWebsocketHandler(PermissionService permissionService) {
        var connectionStatusMessageStream = JdkFlowAdapter.flowPublisherToFlux(permissionService.getConnectionStatusMessageStream());
        connectionStatusMessageStream.subscribe(message -> {
            LOGGER.debug("Incoming messages: {}", message);
            webSocketSessions.forEach(session -> {
                try {
                    session.sendMessage(new TextMessage(message.toString()));
                } catch (IOException e) {
                    LOGGER.error("Error while sending connection status message to WebSocketSession {}", session, e);
                }
            });
        });
    }


    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        LOGGER.debug("Got new WebSocketSession {}", session);
        webSocketSessions.add(session);
    }
}
