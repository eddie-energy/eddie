package energy.eddie.outbound.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

public class CustomDeserializer implements Deserializer<ConsentMarketDocument> {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

    @Override
    public ConsentMarketDocument deserialize(String topic, byte[] data) {
        try {
            return mapper.readValue(data, ConsentMarketDocument.class);
        } catch (IOException e) {
            throw new ConsentMarketDocumentDeserializationException(e);
        }
    }

    public static class ConsentMarketDocumentDeserializationException extends RuntimeException {
        public ConsentMarketDocumentDeserializationException(Throwable cause) {
            super(cause);
        }
    }
}
