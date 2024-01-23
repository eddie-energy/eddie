package energy.eddie.examples.exampleapp.kafka.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.ConnectionStatusMessage;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ConnectionStatusMessageSerde implements Serde<ConnectionStatusMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusMessageSerde.class);
    private final ObjectMapper mapper;

    public ConnectionStatusMessageSerde(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Serializer<ConnectionStatusMessage> serializer() {
        throw new UnsupportedOperationException("This Serde doesn't support serialization!");
    }

    @Override
    public Deserializer<ConnectionStatusMessage> deserializer() {
        return (topic, data) -> {
            try {
                return mapper.readValue(data, ConnectionStatusMessage.class);
            } catch (IOException e) {
                LOGGER.error("Error while deserializing ConnectionStatusMessage.", e);
                return null;
            }
        };
    }
}
