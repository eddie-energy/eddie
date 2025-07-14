package energy.eddie.exampleappbackend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class WebSocketService {
    private final List<WebSocketSecret> tokens = new ArrayList<>();
    private final Map<String, List<String>> activeSessions = new HashMap<>();

    public String createNewSecretForTopic(String topic) {
        var token = WebSocketSecret.builder()
                .secret(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(600))
                .allowedTopic(topic)
                .build();

        tokens.add(token);
        return token.secret();
    }

    public boolean topicHasActiveSession(String topic) {
        return activeSessions.containsKey(topic) && !activeSessions.get(topic).isEmpty();
    }

    public void removeSession(String sessionId) {
        activeSessions.forEach((topic, sessions) -> sessions.removeIf(sessionId::equals));
    }

    public void registerSession(String topic, String sessionId) {
        if (activeSessions.containsKey(topic)) {
            activeSessions.get(topic).add(sessionId);
        } else {
            activeSessions.put(topic, new ArrayList<>(List.of(sessionId)));
        }
    }

    public boolean verifySecret(String secret, String topic) {
        var tokensWithKey = tokens.stream()
                .filter(webSocketToken -> webSocketToken.secret().equals(secret))
                .toList();

        for (var token : tokensWithKey) {
            if (token.allowedTopic().equals(topic)) {
                tokens.remove(token);
                return true;
            }
        }
        return false;
    }

    @Scheduled(cron = "0 * * * * *")
    private void cleanupTokens() {
        tokens.removeIf(token -> Instant.now().isAfter(token.expiresAt()));
    }
}
