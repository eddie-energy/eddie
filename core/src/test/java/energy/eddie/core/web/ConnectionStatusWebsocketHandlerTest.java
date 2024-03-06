package energy.eddie.core.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.core.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConnectionStatusWebsocketHandlerTest {
    @Test
    void givenConnectionStatusMessage_isSentToAllSubscribedWebSocketSessions() throws IOException {
        Sinks.Many<ConnectionStatusMessage> statusMessageSink = Sinks.many().unicast().onBackpressureBuffer();
        PermissionService permissionService = new PermissionService();
        permissionService.registerProvider(new Mvp1ConnectionStatusMessageProvider() {
            @Override
            public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
                return statusMessageSink.asFlux();
            }

            @Override
            public void close() {
                statusMessageSink.tryEmitComplete();
            }
        });
        var mockSession1 = mock(WebSocketSession.class);
        var mockSession2 = mock(WebSocketSession.class);

        // Given
        var websocketHandler = new ConnectionStatusWebsocketHandler(permissionService);

        // When
        websocketHandler.afterConnectionEstablished(mockSession1);
        websocketHandler.afterConnectionEstablished(mockSession2);
        statusMessageSink.tryEmitNext(new ConnectionStatusMessage("one", "one", "one", mock(DataSourceInformation.class), PermissionProcessStatus.CREATED));

        // Then
        verify(mockSession1).sendMessage(any());
        verify(mockSession2).sendMessage(any());
    }
}
