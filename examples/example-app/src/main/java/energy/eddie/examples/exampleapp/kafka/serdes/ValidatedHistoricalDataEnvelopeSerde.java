package energy.eddie.examples.exampleapp.kafka.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ValidatedHistoricalDataEnvelopeSerde implements Serde<ValidatedHistoricalDataEnvelope> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataEnvelopeSerde.class);
    private final ObjectMapper mapper;

    public ValidatedHistoricalDataEnvelopeSerde(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Serializer<ValidatedHistoricalDataEnvelope> serializer() {
        throw new UnsupportedOperationException("This Serde doesn't support serialization!");
    }

    @Override
    public Deserializer<ValidatedHistoricalDataEnvelope> deserializer() {
        return (topic, data) -> {
            try {
                return mapper.readValue(data, ValidatedHistoricalDataEnvelope.class);
            } catch (IOException e) {
                LOGGER.error("Error while deserializing EddieValidatedHistoricalDataMarketDocument.", e);
                return null;
            }
        };
    }
}
