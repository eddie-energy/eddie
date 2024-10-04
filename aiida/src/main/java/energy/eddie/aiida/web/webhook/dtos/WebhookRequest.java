package energy.eddie.aiida.web.webhook.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "action",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientConnAckRequest.class, name = "client_connack"),
        @JsonSubTypes.Type(value = ClientDisconnectedRequest.class, name = "client_disconnected"),
})

@SuppressWarnings("NullAway")
public abstract class WebhookRequest {
    @JsonProperty
    private SupportedWebhookActions action;
    @JsonProperty("clientid")
    private String clientId;
    @JsonProperty
    private String username;

    public SupportedWebhookActions action() {
        return action;
    }

    public String clientId() {
        return clientId;
    }
}
