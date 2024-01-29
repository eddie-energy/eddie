package energy.eddie.examples.exampleapp.kafka.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ConsentMarketDocumentSerde implements Serde<ConsentMarketDocument> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentMarketDocumentSerde.class);
    private final ObjectMapper mapper;

    public ConsentMarketDocumentSerde(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Serializer<ConsentMarketDocument> serializer() {
        throw new UnsupportedOperationException("This Serde doesn't support serialization!");
    }

    @Override
    public Deserializer<ConsentMarketDocument> deserializer() {
        return (topic, data) -> {
            try {
                return mapper.readValue(data, ConsentMarketDocument.class);
            } catch (IOException e) {
                LOGGER.error("Error while deserializing ConnectionStatusMessage.", e);
                return null;
            }
        };
    }
}
