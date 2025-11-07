package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Publisher;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.MessageWithHeaders;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.outbound.ConnectionStatusMessageOutboundConnector;
import energy.eddie.api.agnostic.outbound.RawDataOutboundConnector;
import energy.eddie.api.v0_82.outbound.AccountingPointEnvelopeOutboundConnector;
import energy.eddie.api.v0_82.outbound.PermissionMarketDocumentOutboundConnector;
import energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector;
import energy.eddie.cim.serde.MessageSerde;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.shared.Headers;
import energy.eddie.outbound.shared.TopicConfiguration;
import energy.eddie.outbound.shared.TopicStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class AmqpOutbound implements
        AutoCloseable,
        ConnectionStatusMessageOutboundConnector,
        RawDataOutboundConnector,
        PermissionMarketDocumentOutboundConnector,
        ValidatedHistoricalDataEnvelopeOutboundConnector,
        AccountingPointEnvelopeOutboundConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpOutbound.class);
    private final Publisher publisher;
    private final MessageSerde serde;
    private final TopicConfiguration config;

    public AmqpOutbound(Connection connection, MessageSerde serde, TopicConfiguration config) {
        publisher = connection.publisherBuilder().build();
        this.serde = serde;
        this.config = config;
    }

    @Override
    public void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> connectionStatusMessageStream) {
        connectionStatusMessageStream
                .subscribe(publish(config.connectionStatusMessage(), AmqpOutbound::toHeaders));
    }

    @Override
    public void setRawDataStream(Flux<RawDataMessage> rawDataStream) {
        rawDataStream
                .subscribe(publish(config.rawDataMessage(), AmqpOutbound::toHeaders));
    }

    @Override
    public void setPermissionMarketDocumentStream(Flux<PermissionEnvelope> permissionMarketDocumentStream) {
        permissionMarketDocumentStream
                .subscribe(publish(config.permissionMarketDocument(), AmqpOutbound::toHeaders));
    }

    @Override
    public void setAccountingPointEnvelopeStream(Flux<AccountingPointEnvelope> marketDocumentStream) {
        marketDocumentStream
                .subscribe(publish(config.accountingPointMarketDocument(), AmqpOutbound::toHeaders));
    }


    @Override
    public void setEddieValidatedHistoricalDataMarketDocumentStream(Flux<ValidatedHistoricalDataEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(publish(config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_0_82), AmqpOutbound::toHeaders));
    }

    @Override
    public void close() {
        publisher.close();
    }


    private void publish(
            Object payload,
            String exchange,
            Map<String, String> headers
    ) {
        try {
            var message = publisher
                    .message()
                    .body(serde.serialize(payload));
            for (var entry : headers.entrySet()) {
                message.property(entry.getKey(), entry.getValue());
            }
            message = message
                    .toAddress()
                    .exchange(exchange)
                    .message();
            publisher.publish(message, this::callback);
        } catch (Exception e) {
            LOGGER.warn("Could not parse message for exchange {}", exchange, e);
        }
    }

    private <T> Consumer<T> publish(String exchange, Function<T, Map<String, String>> toHeaders) {
        return payload -> publish(payload, exchange, toHeaders.apply(payload));
    }

    private void callback(Publisher.Context context) {
        switch (context.status()) {
            case ACCEPTED -> LOGGER.debug("Broker accepted message {}", context.message());
            case REJECTED -> LOGGER.warn("Broker rejected message {}", context.message());
            case RELEASED -> LOGGER.info("Consumer did not process message, will be re-queued {}", context.message());
        }
    }

    private static Map<String, String> toHeaders(MessageWithHeaders raw) {
        return Map.of(
                Headers.PERMISSION_ID, raw.permissionId(),
                Headers.CONNECTION_ID, raw.connectionId(),
                Headers.DATA_NEED_ID, raw.dataNeedId()
        );
    }

    private static Map<String, String> toHeaders(PermissionEnvelope pmd) {
        var header = pmd.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        return Map.of(
                Headers.PERMISSION_ID, header.getPermissionid(),
                Headers.CONNECTION_ID, header.getConnectionid(),
                Headers.DATA_NEED_ID, header.getDataNeedid()
        );
    }

    private static Map<String, String> toHeaders(AccountingPointEnvelope envelope) {
        var header = envelope.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        return Map.of(
                Headers.PERMISSION_ID, header.getPermissionid(),
                Headers.CONNECTION_ID, header.getConnectionid(),
                Headers.DATA_NEED_ID, header.getDataNeedid()
        );
    }

    private static Map<String, String> toHeaders(ValidatedHistoricalDataEnvelope envelope) {
        var header = envelope.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        return Map.of(
                Headers.PERMISSION_ID, header.getPermissionid(),
                Headers.CONNECTION_ID, header.getConnectionid(),
                Headers.DATA_NEED_ID, header.getDataNeedid()
        );
    }
}
