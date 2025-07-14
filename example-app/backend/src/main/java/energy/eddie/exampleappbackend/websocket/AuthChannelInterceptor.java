package energy.eddie.exampleappbackend.websocket;

import energy.eddie.exampleappbackend.exception.WebSocketAuthenticationException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {
    private static final Pattern RTD_TOPIC_PATTERN = Pattern.compile("^/topic/real-time-data/(\\d+)$");
    private final WebSocketService webSocketService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            var topic = accessor.getDestination();
            if (topic == null || topic.isEmpty()) {
                log.warn("Websocket session with missing topic header was established! Denying connection!");
                throw new WebSocketAuthenticationException("Missing topic in subscription message!");
            }
            var rtdTopicMatcher = RTD_TOPIC_PATTERN.matcher(topic);

            if (rtdTopicMatcher.matches()) {
                var secret = accessor.getFirstNativeHeader("secret");
                if (secret == null || secret.isEmpty()) {
                    log.warn("Websocket session with missing secret header was established! Denying connection!");
                    throw new WebSocketAuthenticationException("Missing secret in subscription message!");
                }
                if (webSocketService.verifySecret(secret, topic)) {
                    var sessionId = accessor.getSessionId();
                    webSocketService.registerSession(topic, sessionId);
                    log.info("Successfully authenticated websocket session with id {} for topic {}!", sessionId, topic);
                } else {
                    log.warn("Websocket session with invalid secret header was established! Denying connection!");
                    throw new WebSocketAuthenticationException("Invalid secret in subscription message!");
                }
            } else {
                log.warn("Websocket session with invalid topic header was established! Denying connection!");
                throw new WebSocketAuthenticationException("Invalid Topic!");
            }
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            var sessionId = accessor.getSessionId();
            log.info("Ended websocket session with id {}!", accessor.getSessionId());
            webSocketService.removeSession(sessionId);
        }

        return message;
    }
}
