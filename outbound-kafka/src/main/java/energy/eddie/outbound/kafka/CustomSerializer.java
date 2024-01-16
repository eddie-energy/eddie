package energy.eddie.outbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

class CustomSerializer implements Serializer<Object> {
    private final StringSerializer stringSerializer = new StringSerializer();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ObjectMapper vhdObjectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data instanceof ConsumptionRecord || data instanceof ConnectionStatusMessage) {
            return serializeJson(data);
        }
        if (data instanceof EddieValidatedHistoricalDataMarketDocument marketDocument) {
            return serializeEddieValidatedHistoricalDataMarketDocument(marketDocument);
        }
        if (data instanceof RawDataMessage rawDataMessage) {
            return serializeRawDataMessage(rawDataMessage);
        }

        if (data == null) {
            return new byte[0];
        }
        throw new UnsupportedOperationException("Unsupported object type: " + data.getClass());
    }

    private byte[] serializeRawDataMessage(RawDataMessage message) {
        try {
            // use vhdObjectMapper to make timestamps human-readable
            return vhdObjectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new RawDataMessageSerializationException(e);
        }
    }


    private byte[] serializeJson(Object data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new ConsumptionRecordSerializationException(e);
        }
    }

    private byte[] serializeEddieValidatedHistoricalDataMarketDocument(EddieValidatedHistoricalDataMarketDocument data) {
        try {
            return vhdObjectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new EddieValidatedHistoricalDataMarketDocumentSerializationException(e);
        }
    }


    @Override
    public void close() {
        stringSerializer.close();
    }

    public static class ConsumptionRecordSerializationException extends RuntimeException {
        public ConsumptionRecordSerializationException(Throwable cause) {
            super(cause);
        }
    }

    public static class EddieValidatedHistoricalDataMarketDocumentSerializationException extends RuntimeException {
        public EddieValidatedHistoricalDataMarketDocumentSerializationException(Throwable cause) {
            super(cause);
        }
    }

    public static class RawDataMessageSerializationException extends RuntimeException {
        public RawDataMessageSerializationException(Throwable cause) {
            super(cause);
        }
    }
}