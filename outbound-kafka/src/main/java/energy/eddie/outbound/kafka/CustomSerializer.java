package energy.eddie.outbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import energy.eddie.cim.v0_82.pmd.PermissionEnveloppe;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnveloppe;
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
        return switch (data) {
            case ConsumptionRecord ignored -> serializeJson(data);
            case ConnectionStatusMessage ignored -> serializeJson(data);
            case ValidatedHistoricalDataEnveloppe vhd ->
                    serializeEddieValidatedHistoricalDataMarketDocument(vhd);
            case PermissionEnveloppe pmd -> serializePermissionMarketDocument(pmd);
            case RawDataMessage rawDataMessage -> serializeRawDataMessage(rawDataMessage);
            case EddieAccountingPointMarketDocument accountingPointMarketDocument ->
                    serializeEddieAccountingPointMarketDocument(accountingPointMarketDocument);
            case null -> new byte[0];
            default -> throw new UnsupportedOperationException("Unsupported object type: " + data.getClass());
        };
    }

    private byte[] serializeJson(Object data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new ConsumptionRecordSerializationException(e);
        }
    }

    private byte[] serializeEddieValidatedHistoricalDataMarketDocument(ValidatedHistoricalDataEnveloppe data) {
        try {
            return vhdObjectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new ValidatedHistoricalDataEnveloppeSerializationException(e);
        }
    }

    private byte[] serializePermissionMarketDocument(PermissionEnveloppe pmd) {
        try {
            return vhdObjectMapper.writeValueAsBytes(pmd);
        } catch (JsonProcessingException e) {
            throw new PermissionMarketDocumentSerializationException(e);
        }
    }

    private byte[] serializeRawDataMessage(RawDataMessage message) {
        try {
            // use vhdObjectMapper to make timestamps human-readable
            return vhdObjectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new RawDataMessageSerializationException(e);
        }
    }

    private byte[] serializeEddieAccountingPointMarketDocument(EddieAccountingPointMarketDocument data) {
        try {
            return vhdObjectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new EddieAccountingPointMarketDocumentSerializationException(e);
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

    public static class ValidatedHistoricalDataEnveloppeSerializationException extends RuntimeException {
        public ValidatedHistoricalDataEnveloppeSerializationException(Throwable cause) {
            super(cause);
        }
    }


    public static class EddieAccountingPointMarketDocumentSerializationException extends RuntimeException {
        public EddieAccountingPointMarketDocumentSerializationException(Throwable cause) {
            super(cause);
        }
    }

    public static class PermissionMarketDocumentSerializationException extends RuntimeException {
        public PermissionMarketDocumentSerializationException(Throwable cause) {
            super(cause);
        }
    }

    public static class RawDataMessageSerializationException extends RuntimeException {
        public RawDataMessageSerializationException(Throwable cause) {
            super(cause);
        }
    }
}
