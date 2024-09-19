package energy.eddie.aiida.web.webhook.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("NullAway")
public class ClientDisconnectedRequest extends WebhookRequest {
    @JsonProperty
    private String reason;

    public String reason() {
        return reason;
    }
}
