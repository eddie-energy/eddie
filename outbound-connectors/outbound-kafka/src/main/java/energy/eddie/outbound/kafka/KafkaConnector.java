// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.MessageWithHeaders;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.agnostic.RawDataMessage;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.ap.MessageDocumentHeaderMetaInformationComplexType;
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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class KafkaConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConnector.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TopicConfiguration config;

    public KafkaConnector(KafkaTemplate<String, Object> kafkaTemplate, TopicConfiguration config) {
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
    }

    @MessageStream(ConnectionStatusMessage.class)
    public void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> statusMessageStream) {
        statusMessageStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceStatusMessage)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(PermissionEnvelope.class)
    public void setPermissionMarketDocumentStream(Flux<PermissionEnvelope> permissionMarketDocumentStream) {
        permissionMarketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::producePermissionMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public void setEddieValidatedHistoricalDataMarketDocumentStream(
            Flux<ValidatedHistoricalDataEnvelope> marketDocumentStream
    ) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceEddieValidatedHistoricalDataMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(RawDataMessage.class)
    public void setRawDataStream(Flux<RawDataMessage> rawDataStream) {
        rawDataStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceRawDataMessage)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(OpaqueEnvelope.class)
    public void setOpaqueEnvelopeStream(Flux<OpaqueEnvelope> opaqueEnvelopeStream) {
        opaqueEnvelopeStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceOpaqueEnvelope)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(AccountingPointEnvelope.class)
    public void setAccountingPointEnvelopeStream(Flux<AccountingPointEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceAccountingPointEnvelope)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(VHDEnvelope.class)
    public void setValidatedHistoricalDataMarketDocumentStream(Flux<VHDEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceValidatedHistoricalDataMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }


    @MessageStream(energy.eddie.cim.v1_04.rtd.RTDEnvelope.class)
    @SuppressWarnings("java:S100")
    public void setNearRealTimeDataMarketDocumentStreamV1_04(Flux<energy.eddie.cim.v1_04.rtd.RTDEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceNearRealTimeDataMarketDocumentV104)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(energy.eddie.cim.v1_12.rtd.RTDEnvelope.class)
    @SuppressWarnings("java:S100")
    public void setNearRealTimeDataMarketDocumentStreamV1_12(Flux<energy.eddie.cim.v1_12.rtd.RTDEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceNearRealTimeDataMarketDocumentV112)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(AcknowledgementEnvelope.class)
    public void setAcknowledgementMarketDocumentStream(Flux<AcknowledgementEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceAcknowledgementMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(RECMMOEEnvelope.class)
    public void setMinMaxEnvelopeStream(Flux<RECMMOEEnvelope> minMaxEnvelopeStream) {
        minMaxEnvelopeStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceMinMaxEnvelope)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(ESRDMDEnvelope.class)
    public void setEnergySharingReferenceDataMarketDocumentStream(Flux<ESRDMDEnvelope> marketDocumentStream) {
        LOGGER.info("Setting stream for ESRDMD");
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceEnergySharingReferenceDataMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @MessageStream(RequestPermissionEnvelope.class)
    public void setRequestPermissionMarketDocumentStream(Flux<RequestPermissionEnvelope> marketDocumentStream) {
        LOGGER.info("Setting stream for Request Permission Market Document");
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceRequestPermissionMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    private void produceRequestPermissionMarketDocument(RequestPermissionEnvelope rpmdEnvelope) {
        var header = rpmdEnvelope.getMessageDocumentHeader()
                                 .getMetaInformation();
        var permissionId = header.getRequestPermissionId();
        var metaInformation = rpmdEnvelope.getMessageDocumentHeader().getMetaInformation();
        var toSend = new ProducerRecord<String, Object>(
                config.requestPermissionMarketDocument(),
                null,
                permissionId,
                rpmdEnvelope,
                List.of(
                        new StringHeader(Headers.PERMISSION_ID, metaInformation.getRequestPermissionId()),
                        new StringHeader(Headers.CONNECTION_ID, metaInformation.getConnectionId()),
                        new StringHeader(Headers.DATA_NEED_ID, metaInformation.getDataNeedId())
                )
        );
        sendToKafka(toSend, "Could not produce request permission market document");
    }

    private void produceEnergySharingReferenceDataMarketDocument(ESRDMDEnvelope esrdmdEnvelope) {
        var header = esrdmdEnvelope.getMessageDocumentHeader()
                                   .getMetaInformation();
        var permissionId = header.getRequestPermissionId();
        var metaInformation = esrdmdEnvelope.getMessageDocumentHeader().getMetaInformation();
        var toSend = new ProducerRecord<String, Object>(
                config.energySharingReferenceDataMarketDocument(),
                null,
                permissionId,
                esrdmdEnvelope,
                List.of(
                        new StringHeader(Headers.PERMISSION_ID, metaInformation.getRequestPermissionId()),
                        new StringHeader(Headers.CONNECTION_ID, metaInformation.getConnectionId()),
                        new StringHeader(Headers.DATA_NEED_ID, metaInformation.getDataNeedId())
                )
        );
        sendToKafka(toSend, "Could not produce energy sharing reference data market document");
    }


    private void produceStatusMessage(ConnectionStatusMessage statusMessage) {
        var toSend = new ProducerRecord<String, Object>(
                config.connectionStatusMessage(),
                statusMessage
        );
        sendToKafka(toSend, "Could not produce connection status message");
        LOGGER.debug("Produced connection status {} message for permission request {}",
                     statusMessage.status(),
                     statusMessage.permissionId());
    }

    private void producePermissionMarketDocument(PermissionEnvelope permissionMarketDocument) {
        var header = permissionMarketDocument.getMessageDocumentHeader()
                                             .getMessageDocumentHeaderMetaInformation();
        var permissionId = header.getPermissionid();
        var toSend = new ProducerRecord<String, Object>(
                config.permissionMarketDocument(),
                null,
                permissionId,
                permissionMarketDocument,
                cimToHeaders(header)
        );
        sendToKafka(toSend, "Could not produce permission market document");
    }

    private Iterable<Header> cimToHeaders(energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderMetaInformationComplexType header) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getPermissionid()),
                new StringHeader(Headers.CONNECTION_ID, header.getConnectionid()),
                new StringHeader(Headers.DATA_NEED_ID, header.getDataNeedid())
        );
    }

    private void produceEddieValidatedHistoricalDataMarketDocument(
            ValidatedHistoricalDataEnvelope marketDocument
    ) {
        var info = marketDocument.getMessageDocumentHeader()
                                 .getMessageDocumentHeaderMetaInformation();
        var toSend = new ProducerRecord<String, Object>(config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_0_82),
                                                        null,
                                                        info.getConnectionid(),
                                                        marketDocument,
                                                        cimToHeaders(info));
        sendToKafka(toSend, "Could not produce validated historical data market document message");
        LOGGER.debug("Produced validated historical data market document message for permission request {}",
                     info.getPermissionid());
    }

    private static Iterable<Header> cimToHeaders(energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderMetaInformationComplexType header) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getPermissionid()),
                new StringHeader(Headers.CONNECTION_ID, header.getConnectionid()),
                new StringHeader(Headers.DATA_NEED_ID, header.getDataNeedid())
        );
    }

    @SuppressWarnings("LoggingSimilarMessage")
    private void produceValidatedHistoricalDataMarketDocument(VHDEnvelope vhdEnvelope) {
        var toSend = new ProducerRecord<String, Object>(config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_1_04),
                                                        null,
                                                        vhdEnvelope.getMessageDocumentHeaderMetaInformationConnectionId(),
                                                        vhdEnvelope,
                                                        cimToHeaders(vhdEnvelope));
        sendToKafka(toSend, "Could not produce validated historical data market document message");
        LOGGER.debug("Produced validated historical data market document message for permission request {}",
                     vhdEnvelope.getMessageDocumentHeaderMetaInformationPermissionId());
    }

    private static Iterable<Header> cimToHeaders(energy.eddie.cim.v1_04.vhd.VHDEnvelope header) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getMessageDocumentHeaderMetaInformationPermissionId()),
                new StringHeader(Headers.CONNECTION_ID, header.getMessageDocumentHeaderMetaInformationConnectionId()),
                new StringHeader(Headers.DATA_NEED_ID, header.getMessageDocumentHeaderMetaInformationDataNeedId())
        );
    }

    private void produceRawDataMessage(RawDataMessage message) {
        var toSend = new ProducerRecord<String, Object>(config.rawDataMessage(),
                                                        message.connectionId(),
                                                        message);
        sendToKafka(toSend, "Could not produce raw data message");
        LOGGER.debug("Produced raw data message for permission request {}", message.permissionId());
    }

    private void produceOpaqueEnvelope(OpaqueEnvelope opaqueEnvelope) {
        var toSend = new ProducerRecord<String, Object>(
                config.opaqueEnvelope(),
                null,
                opaqueEnvelope.connectionId(),
                opaqueEnvelope,
                toHeaders(opaqueEnvelope)
        );
        sendToKafka(toSend, "Could not produce opaque envelope message");
        LOGGER.debug("Produced opaque envelope message for permission request {}", opaqueEnvelope.permissionId());
    }

    private void produceAccountingPointEnvelope(AccountingPointEnvelope marketDocument) {
        var header = marketDocument.getMessageDocumentHeader()
                                   .getMessageDocumentHeaderMetaInformation();
        var toSend = new ProducerRecord<String, Object>(
                config.accountingPointMarketDocument(),
                null,
                header.getConnectionid(),
                marketDocument,
                cimToHeaders(header)
        );
        sendToKafka(toSend, "Could not produce accounting point market document message");
        LOGGER.debug("Produced accounting point market document message for permission request {}",
                     header.getPermissionid());
    }

    private void produceNearRealTimeDataMarketDocumentV104(energy.eddie.cim.v1_04.rtd.RTDEnvelope marketDocument) {
        var toSend = new ProducerRecord<String, Object>(
                config.nearRealTimeDataMarketDocument(TopicStructure.DataModels.CIM_1_04),
                null,
                marketDocument.getMessageDocumentHeaderMetaInformationConnectionId(),
                marketDocument,
                cimToHeaders(marketDocument)
        );
        sendToKafka(toSend,
                    "Could not produce " + TopicStructure.DataModels.CIM_1_04 + " near real-time market document message");
        LOGGER.debug("Produced {} near real-time market document message for permission request {}",
                     TopicStructure.DataModels.CIM_1_04,
                     marketDocument.getMessageDocumentHeaderMetaInformationPermissionId());
    }

    private void produceNearRealTimeDataMarketDocumentV112(energy.eddie.cim.v1_12.rtd.RTDEnvelope marketDocument) {
        var metaInformation = marketDocument.getMessageDocumentHeader().getMetaInformation();
        var toSend = new ProducerRecord<String, Object>(
                config.nearRealTimeDataMarketDocument(TopicStructure.DataModels.CIM_1_12),
                null,
                metaInformation.getConnectionId(),
                marketDocument,
                cimToHeaders(marketDocument)
        );
        sendToKafka(toSend,
                    "Could not produce " + TopicStructure.DataModels.CIM_1_12 + " near real-time market document message");
        LOGGER.debug("Produced {} near real-time market document message for permission request {}",
                     TopicStructure.DataModels.CIM_1_12,
                     metaInformation.getRequestPermissionId());
    }

    private void produceAcknowledgementMarketDocument(AcknowledgementEnvelope marketDocument) {
        var metaInformation = marketDocument.getMessageDocumentHeader().getMetaInformation();
        var toSend = new ProducerRecord<String, Object>(
                config.acknowledgementMarketDocument(),
                null,
                metaInformation.getConnectionId(),
                marketDocument,
                cimToHeaders(marketDocument)
        );
        sendToKafka(toSend, "Could not produce acknowledgement market document message");
        LOGGER.debug("Produced acknowledgement market document message for permission request {}",
                     metaInformation.getRequestPermissionId());
    }

    private void produceMinMaxEnvelope(RECMMOEEnvelope minMaxEnvelope) {
        var metaInformation = minMaxEnvelope.getMessageDocumentHeader().getMetaInformation();
        var toSend = new ProducerRecord<String, Object>(
                config.minMaxEnvelopeDocument(),
                null,
                metaInformation.getConnectionId(),
                minMaxEnvelope,
                cimToHeaders(minMaxEnvelope)
        );
        sendToKafka(toSend, "Could not produce min-max envelope message");
        LOGGER.debug("Produced min-max envelope message for permission request {}",
                     metaInformation.getRequestPermissionId());
    }

    private Iterable<Header> cimToHeaders(energy.eddie.cim.v1_04.rtd.RTDEnvelope header) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getMessageDocumentHeaderMetaInformationPermissionId()),
                new StringHeader(Headers.CONNECTION_ID, header.getMessageDocumentHeaderMetaInformationConnectionId()),
                new StringHeader(Headers.DATA_NEED_ID, header.getMessageDocumentHeaderMetaInformationDataNeedId())
        );
    }

    private Iterable<Header> cimToHeaders(energy.eddie.cim.v1_12.rtd.RTDEnvelope header) {
        var metaInformation = header.getMessageDocumentHeader().getMetaInformation();
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, metaInformation.getRequestPermissionId()),
                new StringHeader(Headers.CONNECTION_ID, metaInformation.getConnectionId()),
                new StringHeader(Headers.DATA_NEED_ID, metaInformation.getDataNeedId())
        );
    }

    private Iterable<Header> cimToHeaders(AcknowledgementEnvelope header) {
        var metaInformation = header.getMessageDocumentHeader().getMetaInformation();
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, metaInformation.getRequestPermissionId()),
                new StringHeader(Headers.CONNECTION_ID, metaInformation.getConnectionId()),
                new StringHeader(Headers.DATA_NEED_ID, metaInformation.getDataNeedId())
        );
    }

    private Iterable<Header> cimToHeaders(RECMMOEEnvelope header) {
        var metaInformation = header.getMessageDocumentHeader().getMetaInformation();
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, metaInformation.getRequestPermissionId()),
                new StringHeader(Headers.CONNECTION_ID, metaInformation.getConnectionId()),
                new StringHeader(Headers.DATA_NEED_ID, metaInformation.getDataNeedId())
        );
    }

    private static List<Header> toHeaders(MessageWithHeaders raw) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, raw.permissionId()),
                new StringHeader(Headers.CONNECTION_ID, raw.connectionId()),
                new StringHeader(Headers.DATA_NEED_ID, raw.dataNeedId())
        );
    }

    private void sendToKafka(ProducerRecord<String, Object> toSend, String errorMessage) {
        Mono.fromFuture(kafkaTemplate.send(toSend))
            .subscribe(
                    result -> LOGGER.debug("Produced kafka message: {}", result),
                    ex -> LOGGER.warn(errorMessage, ex)
            );
    }

    private void logStreamerError(Throwable throwable, Object obj) {
        LOGGER.warn("Error processing stream object: {}", obj, throwable);
    }

    private static List<Header> cimToHeaders(
            MessageDocumentHeaderMetaInformationComplexType header
    ) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getPermissionid()),
                new StringHeader(Headers.CONNECTION_ID, header.getConnectionid()),
                new StringHeader(Headers.DATA_NEED_ID, header.getDataNeedid())
        );
    }

    private record StringHeader(String key, String payload) implements Header {
        @Override
        public byte[] value() {
            return payload.getBytes(StandardCharsets.UTF_8);
        }
    }
}
