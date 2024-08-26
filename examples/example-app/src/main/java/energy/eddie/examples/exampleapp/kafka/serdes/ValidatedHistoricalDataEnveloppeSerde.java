package energy.eddie.examples.exampleapp.kafka.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnveloppe;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ValidatedHistoricalDataEnveloppeSerde implements Serde<ValidatedHistoricalDataEnveloppe> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataEnveloppeSerde.class);
    private final ObjectMapper mapper;

    public ValidatedHistoricalDataEnveloppeSerde(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Serializer<ValidatedHistoricalDataEnveloppe> serializer() {
        throw new UnsupportedOperationException("This Serde doesn't support serialization!");
    }

    @Override
    public Deserializer<ValidatedHistoricalDataEnveloppe> deserializer() {
        return (topic, data) -> {
            try {
                return mapper.readValue(data, ValidatedHistoricalDataEnveloppe.class);
            } catch (IOException e) {
                LOGGER.error("Error while deserializing EddieValidatedHistoricalDataMarketDocument.", e);
                return null;
            }
        };
    }
}
