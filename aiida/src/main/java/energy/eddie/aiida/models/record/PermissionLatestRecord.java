package energy.eddie.aiida.models.record;

import energy.eddie.dataneeds.needs.aiida.AiidaSchema;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PermissionLatestRecord {
    private final String topic;
    private final String serverUri;
    private final ConcurrentHashMap<AiidaSchema, LatestRecordSchema> messages;

    public PermissionLatestRecord(String topic, String serverUri) {
        this.topic = topic;
        this.serverUri = serverUri;
        this.messages = new ConcurrentHashMap<>();
    }

    public void putSchema(AiidaSchema schema, LatestRecordSchema message) {
        messages.put(schema, message);
    }

    public ConcurrentMap<AiidaSchema, LatestRecordSchema> messages() {
        return messages;
    }

    public String topic() {
        return topic;
    }

    public String serverUri() {
        return serverUri;
    }
}