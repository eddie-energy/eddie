package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.shared.serde.MessageSerde;
import energy.eddie.outbound.shared.serde.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

class CustomSerializer implements Serializer<Object> {
    private final StringSerializer stringSerializer = new StringSerializer();
    private final MessageSerde serde;

    CustomSerializer(MessageSerde serde) {
        this.serde = serde;
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        return switch (data) {
            case ConnectionStatusMessage csm -> serializeConnectionStatusMessage(csm);
            case ValidatedHistoricalDataEnvelope vhd -> serializeEddieValidatedHistoricalDataMarketDocument(vhd);
            case PermissionEnvelope pmd -> serializePermissionMarketDocument(pmd);
            case RawDataMessage rawDataMessage -> serializeRawDataMessage(rawDataMessage);
            case AccountingPointEnvelope accountingPointMarketDocument ->
                    serializeAccountingPointEnvelope(accountingPointMarketDocument);
            case null -> new byte[0];
            default -> throw new UnsupportedOperationException("Unsupported object type: " + data.getClass());
        };
    }

    @Override
    public void close() {
        stringSerializer.close();
    }

    private byte[] serializeConnectionStatusMessage(ConnectionStatusMessage data) {
        try {
            return serde.serialize(data);
        } catch (SerializationException e) {
            throw new ConnectionStatusMessageSerializationException(e);
        }
    }

    private byte[] serializeEddieValidatedHistoricalDataMarketDocument(ValidatedHistoricalDataEnvelope data) {
        try {
            return serde.serialize(data);
        } catch (SerializationException e) {
            throw new ValidatedHistoricalDataEnvelopeSerializationException(e);
        }
    }

    private byte[] serializePermissionMarketDocument(PermissionEnvelope pmd) {
        try {
            return serde.serialize(pmd);
        } catch (SerializationException e) {
            throw new PermissionMarketDocumentSerializationException(e);
        }
    }

    private byte[] serializeRawDataMessage(RawDataMessage message) {
        try {
            return serde.serialize(message);
        } catch (SerializationException e) {
            throw new RawDataMessageSerializationException(e);
        }
    }

    private byte[] serializeAccountingPointEnvelope(AccountingPointEnvelope data) {
        try {
            return serde.serialize(data);
        } catch (SerializationException e) {
            throw new AccountingPointEnvelopeSerializationException(e);
        }
    }

    public static class ConnectionStatusMessageSerializationException extends RuntimeException {
        public ConnectionStatusMessageSerializationException(Throwable cause) {
            super(cause);
        }
    }

    public static class ValidatedHistoricalDataEnvelopeSerializationException extends RuntimeException {
        public ValidatedHistoricalDataEnvelopeSerializationException(Throwable cause) {
            super(cause);
        }
    }


    public static class AccountingPointEnvelopeSerializationException extends RuntimeException {
        public AccountingPointEnvelopeSerializationException(Throwable cause) {
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
