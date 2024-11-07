package energy.eddie.outbound.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import jakarta.annotation.Nullable;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CustomDeserializer implements Deserializer<PermissionEnvelope> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomDeserializer.class);
    private final ObjectMapper mapper;

    public CustomDeserializer(ObjectMapper mapper) {this.mapper = mapper;}

    @Override
    @Nullable
    public PermissionEnvelope deserialize(String topic, byte[] data) {
        try {
            return mapper.readValue(data, PermissionEnvelope.class);
        } catch (IOException e) {
            LOGGER.info("Got invalid termination document", e);
            return null;
        }
    }
}
