package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.shared.serde.MessageSerde;
import energy.eddie.outbound.shared.serde.SerializationException;
import jakarta.annotation.Nullable;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CustomSerializer implements Serializer<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomSerializer.class);
    private final StringSerializer stringSerializer = new StringSerializer();
    private final MessageSerde serde;

    CustomSerializer(MessageSerde serde) {
        this.serde = serde;
    }

    @Override
    @Nullable
    public byte[] serialize(String topic, Object data) {
        return switch (data) {
            case ConnectionStatusMessage ignored -> serialize(data);
            case ValidatedHistoricalDataEnvelope ignored -> serialize(data);
            case PermissionEnvelope ignored -> serialize(data);
            case RawDataMessage ignored -> serialize(data);
            case AccountingPointEnvelope ignored -> serialize(data);
            case RTREnvelope ignored -> serialize(data);
            case null -> null;
            default -> {
                LOGGER.warn("Got invalid type to serialize {}", data.getClass());
                yield null;
            }
        };
    }

    @Override
    public void close() {
        stringSerializer.close();
    }

    // Sonar wants us to return an empty array, but the kafka implementations return null, so will do the same
    @SuppressWarnings("java:S1168")
    @Nullable
    private byte[] serialize(Object payload) {
        try {
            return serde.serialize(payload);
        } catch (SerializationException e) {
            LOGGER.warn("Could not serialize message of type {}", payload.getClass(), e);
            return null;
        }
    }
}
