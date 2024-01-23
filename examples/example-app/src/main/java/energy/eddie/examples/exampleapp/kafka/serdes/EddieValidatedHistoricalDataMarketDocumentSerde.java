package energy.eddie.examples.exampleapp.kafka.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EddieValidatedHistoricalDataMarketDocumentSerde implements Serde<EddieValidatedHistoricalDataMarketDocument> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EddieValidatedHistoricalDataMarketDocumentSerde.class);
    private final ObjectMapper mapper;

    public EddieValidatedHistoricalDataMarketDocumentSerde(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Serializer<EddieValidatedHistoricalDataMarketDocument> serializer() {
        throw new UnsupportedOperationException("This Serde doesn't support serialization!");
    }

    @Override
    public Deserializer<EddieValidatedHistoricalDataMarketDocument> deserializer() {
        return (topic, data) -> {
            try {
                return mapper.readValue(data, EddieValidatedHistoricalDataMarketDocument.class);
            } catch (IOException e) {
                LOGGER.error("Error while deserializing EddieValidatedHistoricalDataMarketDocument.", e);
                return null;
            }
        };
    }
}
