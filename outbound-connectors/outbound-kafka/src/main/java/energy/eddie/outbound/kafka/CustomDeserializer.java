package energy.eddie.outbound.kafka;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.outbound.shared.serde.DeserializationException;
import energy.eddie.outbound.shared.serde.MessageSerde;
import jakarta.annotation.Nullable;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDeserializer implements Deserializer<PermissionEnvelope> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomDeserializer.class);
    private final MessageSerde serde;

    public CustomDeserializer(MessageSerde serde) {
        this.serde = serde;
    }

    @Override
    @Nullable
    public PermissionEnvelope deserialize(String topic, byte[] data) {
        try {
            return serde.deserialize(data, PermissionEnvelope.class);
        } catch (DeserializationException e) {
            LOGGER.info("Got invalid termination document", e);
            return null;
        }
    }
}
