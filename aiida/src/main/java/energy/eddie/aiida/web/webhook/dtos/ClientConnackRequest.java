package energy.eddie.aiida.web.webhook.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("NullAway")
public class ClientConnackRequest extends WebhookRequest {
    @JsonProperty("keepalive")
    private int keepAlive;
    @JsonProperty("proto_ver")
    private String protoVer;
    @JsonProperty("conn_ack")
    private ConnectionAcknowledgement connAck;

    public ConnectionAcknowledgement connAck() {
        return connAck;
    }
}
