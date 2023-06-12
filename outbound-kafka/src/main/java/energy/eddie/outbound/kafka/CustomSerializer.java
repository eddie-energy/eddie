package energy.eddie.outbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

class CustomSerializer implements Serializer<Object> {
    private final StringSerializer stringSerializer = new StringSerializer();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data instanceof ConsumptionRecord || data instanceof ConnectionStatusMessage) {
            return serializeJson(data);
        }
        if (data == null) {
            return new byte[0];
        }
        throw new UnsupportedOperationException("Unsupported object type: " + data.getClass());
    }


    private byte[] serializeJson(Object data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new ConsumptionRecordSerializationException(e);
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
}
