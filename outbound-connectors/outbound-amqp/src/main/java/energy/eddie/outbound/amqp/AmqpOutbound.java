// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.amqp;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Publisher;
import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.MessageWithHeaders;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.agnostic.RawDataMessage;
import energy.eddie.cim.serde.MessageSerde;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
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
public class AmqpOutbound implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpOutbound.class);
    private final Publisher publisher;
    private final MessageSerde serde;
    private final TopicConfiguration config;

    public AmqpOutbound(Connection connection, MessageSerde serde, TopicConfiguration config) {
        publisher = connection.publisherBuilder().build();
        this.serde = serde;
        this.config = config;
    }

    @MessageStream(ConnectionStatusMessage.class)
    public void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> connectionStatusMessageStream) {
        connectionStatusMessageStream
                .subscribe(publish(config.connectionStatusMessage(), AmqpOutbound::toHeaders));
    }

    @MessageStream(RawDataMessage.class)
    public void setRawDataStream(Flux<RawDataMessage> rawDataStream) {
        rawDataStream
                .subscribe(publish(config.rawDataMessage(), AmqpOutbound::toHeaders));
    }

    @MessageStream(PermissionEnvelope.class)
    public void setPermissionMarketDocumentStream(Flux<PermissionEnvelope> permissionMarketDocumentStream) {
        permissionMarketDocumentStream
                .subscribe(publish(config.permissionMarketDocument(), AmqpOutbound::toHeaders));
    }

    @MessageStream(AccountingPointEnvelope.class)
    public void setAccountingPointEnvelopeStream(Flux<AccountingPointEnvelope> marketDocumentStream) {
        marketDocumentStream
                .subscribe(publish(config.accountingPointMarketDocument(), AmqpOutbound::toHeaders));
    }


    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public void setEddieValidatedHistoricalDataMarketDocumentStream(Flux<ValidatedHistoricalDataEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(publish(config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_0_82),
                                               AmqpOutbound::toHeaders));
    }

    @MessageStream(energy.eddie.cim.v1_04.rtd.RTDEnvelope.class)
    @SuppressWarnings("java:S100")
    public void setNearRealTimeDataMarketDocumentStreamV1_04(Flux<energy.eddie.cim.v1_04.rtd.RTDEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(publish(config.nearRealTimeDataMarketDocument(TopicStructure.DataModels.CIM_1_04),
                                               AmqpOutbound::toHeaders));
    }

    @MessageStream(AcknowledgementEnvelope.class)
    public void setAcknowledgementMarketDocumentStream(Flux<AcknowledgementEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(publish(config.acknowledgementMarketDocument(), AmqpOutbound::toHeaders));
    }

    @MessageStream(energy.eddie.cim.v1_12.rtd.RTDEnvelope.class)
    @SuppressWarnings("java:S100")
    public void setNearRealTimeDataMarketDocumentStreamV1_12(Flux<energy.eddie.cim.v1_12.rtd.RTDEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(publish(config.nearRealTimeDataMarketDocument(TopicStructure.DataModels.CIM_1_12),
                                               AmqpOutbound::toHeaders));
    }

    @Override
    public void close() {
        publisher.close();
    }

    @MessageStream(VHDEnvelope.class)
    public void setValidatedHistoricalDataMarketDocumentStream(Flux<VHDEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(publish(config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_1_04),
                                               AmqpOutbound::toHeaders));
    }

    @MessageStream(ESRDMDEnvelope.class)
    public void setEnergySharingReferenceDataMarketDocumentStream(Flux<ESRDMDEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(publish(config.energySharingReferenceDataMarketDocument(),
                                               AmqpOutbound::toHeaders));
    }

    @MessageStream(RequestPermissionEnvelope.class)
    public void setRequestPermissionMarketDocumentStream(Flux<RequestPermissionEnvelope> marketDocumentStream) {
        marketDocumentStream.subscribe(publish(config.requestPermissionMarketDocument(),
                                               AmqpOutbound::toHeaders));
    }

    @MessageStream(OpaqueEnvelope.class)
    public void setForwardedOpaqueEnvelopeStream(Flux<OpaqueEnvelope> forwardedOpaqueEnvelopeStream) {
        forwardedOpaqueEnvelopeStream
                .subscribe(publish(config.forwardedOpaqueEnvelope(), AmqpOutbound::toHeaders));
    }

    @MessageStream(RECMMOEEnvelope.class)
    public void setForwardedMinMaxEnvelopeStream(Flux<RECMMOEEnvelope> forwardedMinMaxEnvelopeStream) {
        forwardedMinMaxEnvelopeStream.subscribe(publish(config.forwardedMinMaxEnvelopeDocument(),
                                                        AmqpOutbound::toHeaders));
    }

    private void publish(Object payload, String exchange, Map<String, String> headers) {
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
        return toHeaders(raw.permissionId(), raw.connectionId(), raw.dataNeedId());
    }

    private static Map<String, String> toHeaders(PermissionEnvelope pmd) {
        var header = pmd.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        return toHeaders(header.getPermissionid(), header.getConnectionid(), header.getDataNeedid());
    }

    private static Map<String, String> toHeaders(AccountingPointEnvelope envelope) {
        var header = envelope.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        return toHeaders(header.getPermissionid(), header.getConnectionid(), header.getDataNeedid());
    }

    private static Map<String, String> toHeaders(ValidatedHistoricalDataEnvelope envelope) {
        var header = envelope.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        return toHeaders(header.getPermissionid(), header.getConnectionid(), header.getDataNeedid());
    }

    private static Map<String, String> toHeaders(energy.eddie.cim.v1_04.rtd.RTDEnvelope envelope) {
        return toHeaders(
                envelope.getMessageDocumentHeaderMetaInformationPermissionId(),
                envelope.getMessageDocumentHeaderMetaInformationConnectionId(),
                envelope.getMessageDocumentHeaderMetaInformationDataNeedId()
        );
    }

    private static Map<String, String> toHeaders(energy.eddie.cim.v1_12.rtd.RTDEnvelope envelope) {
        var metaInformation = envelope.getMessageDocumentHeader().getMetaInformation();
        return toHeaders(
                metaInformation.getRequestPermissionId(),
                metaInformation.getConnectionId(),
                metaInformation.getDataNeedId()
        );
    }

    private static Map<String, String> toHeaders(AcknowledgementEnvelope envelope) {
        var metaInformation = envelope.getMessageDocumentHeader().getMetaInformation();
        return toHeaders(
                metaInformation.getRequestPermissionId(),
                metaInformation.getConnectionId(),
                metaInformation.getDataNeedId()
        );
    }

    private static Map<String, String> toHeaders(RECMMOEEnvelope envelope) {
        var metaInformation = envelope.getMessageDocumentHeader().getMetaInformation();
        return toHeaders(
                metaInformation.getRequestPermissionId(),
                metaInformation.getConnectionId(),
                metaInformation.getDataNeedId()
        );
    }

    private static Map<String, String> toHeaders(VHDEnvelope vhdEnvelope) {
        return toHeaders(
                vhdEnvelope.getMessageDocumentHeaderMetaInformationPermissionId(),
                vhdEnvelope.getMessageDocumentHeaderMetaInformationConnectionId(),
                vhdEnvelope.getMessageDocumentHeaderMetaInformationDataNeedId()
        );
    }

    private static Map<String, String> toHeaders(ESRDMDEnvelope envelope) {
        var metaInformation = envelope.getMessageDocumentHeader().getMetaInformation();
        return toHeaders(
                metaInformation.getRequestPermissionId(),
                metaInformation.getConnectionId(),
                metaInformation.getDataNeedId()
        );
    }

    private static Map<String, String> toHeaders(RequestPermissionEnvelope envelope) {
        var metaInformation = envelope.getMessageDocumentHeader().getMetaInformation();
        return toHeaders(
                metaInformation.getRequestPermissionId(),
                metaInformation.getConnectionId(),
                metaInformation.getDataNeedId()
        );
    }

    private static Map<String, String> toHeaders(String permissionId, String connectionId, String dataNeedId) {
        return Map.of(
                Headers.PERMISSION_ID, permissionId,
                Headers.CONNECTION_ID, connectionId,
                Headers.DATA_NEED_ID, dataNeedId
        );
    }
}
