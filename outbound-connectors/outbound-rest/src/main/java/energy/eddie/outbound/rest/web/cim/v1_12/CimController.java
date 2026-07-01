// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.cim.v1_12;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v1_12.CimConnector;
import energy.eddie.outbound.rest.dto.v1_12.*;
import energy.eddie.outbound.rest.model.cim.v1_12.*;
import energy.eddie.outbound.rest.persistence.cim.v1_12.*;
import energy.eddie.outbound.rest.persistence.specifications.CimSpecification;
import energy.eddie.outbound.rest.web.EventStream;
import energy.eddie.outbound.shared.TopicStructure;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.outbound.rest.model.ModelWithJsonPayload.payloadsOf;
import static energy.eddie.outbound.rest.web.EventStream.EVENT_STREAM_XML_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController(value = "cimControllerV1_12")
@RequestMapping(TopicStructure.CIM_1_12_VALUE)
@SuppressWarnings("java:S6830")
public class CimController implements CimSwagger {
    private final CimConnector cimConnector;
    private final NearRealTimeDataMarketDocumentRepository rtdRepository;
    private final AcknowledgementMarketDocumentRepository ackRepository;
    private final EnergySharingReferenceDataMarketDocumentRepository esrRepository;
    private final MinMaxEnvelopeMarketDocumentRepository minMaxRepository;
    private final RequestPermissionMarketDocumentRepository requestPermissionMarketDocumentRepository;
    private final EventStream eventStream;

    public CimController(
            CimConnector cimConnector,
            NearRealTimeDataMarketDocumentRepository rtdRepository,
            AcknowledgementMarketDocumentRepository ackRepository,
            EnergySharingReferenceDataMarketDocumentRepository esrRepository,
            MinMaxEnvelopeMarketDocumentRepository minMaxRepository,
            RequestPermissionMarketDocumentRepository requestPermissionMarketDocumentRepository,
            EventStream eventStream
    ) {
        this.cimConnector = cimConnector;
        this.rtdRepository = rtdRepository;
        this.ackRepository = ackRepository;
        this.esrRepository = esrRepository;
        this.minMaxRepository = minMaxRepository;
        this.requestPermissionMarketDocumentRepository = requestPermissionMarketDocumentRepository;
        this.eventStream = eventStream;
    }

    @Override
    @GetMapping(value = "/near-real-time-data-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<RTDEnvelope>> nearRealTimeDataMdSSE() {
        return eventStream.toJson(cimConnector.getNearRealTimeDataMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/near-real-time-data-md", produces = EVENT_STREAM_XML_VALUE)
    public ResponseEntity<Flux<String>> nearRealTimeDataMdSSEXML() {
        return eventStream.toXml(cimConnector.getNearRealTimeDataMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/near-real-time-data-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<NearRealTimeDataMarketDocuments> nearRealTimeDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<NearRealTimeDataMarketDocumentModel> specification = CimSpecification.buildQueryForV1_12(
                permissionId,
                connectionId,
                dataNeedId,
                countryCode,
                regionConnectorId,
                from,
                to
        );
        var all = rtdRepository.findAll(specification);
        var messages = payloadsOf(all);
        return ResponseEntity.ok()
                             .body(new NearRealTimeDataMarketDocuments(messages));
    }

    @Override
    @GetMapping(value = "/acknowledgement-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<AcknowledgementEnvelope>> acknowledgementMdSSE() {
        return eventStream.toJson(cimConnector.getAcknowledgementMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/acknowledgement-md", produces = EVENT_STREAM_XML_VALUE)
    public ResponseEntity<Flux<String>> acknowledgementMdSSEXML() {
        return eventStream.toXml(cimConnector.getAcknowledgementMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/acknowledgement-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<AcknowledgementMarketDocuments> acknowledgementMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<AcknowledgementMarketDocumentModel> specification = CimSpecification.buildQueryForV1_12(
                permissionId,
                connectionId,
                dataNeedId,
                countryCode,
                regionConnectorId,
                from,
                to
        );
        var all = ackRepository.findAll(specification);
        var messages = payloadsOf(all);
        return ResponseEntity.ok()
                             .body(new AcknowledgementMarketDocuments(messages));
    }

    @Override
    @PostMapping(value = "min-max-envelope-md", consumes = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<Void> minMaxEnvelopeMd(@RequestBody RECMMOEEnvelope minMaxEnvelope) {
        cimConnector.publish(minMaxEnvelope);
        return ResponseEntity.accepted()
                             .build();
    }

    @Override
    @GetMapping(value = "/min-max-envelope-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<RECMMOEEnvelope>> minMaxEnvelopeMdSSE() {
        return eventStream.toJson(cimConnector.getForwardedMinMaxEnvelopeStream());
    }

    @Override
    @GetMapping(value = "/min-max-envelope-md", produces = EVENT_STREAM_XML_VALUE)
    public ResponseEntity<Flux<String>> minMaxEnvelopeMdSSEXML() {
        return eventStream.toXml(cimConnector.getForwardedMinMaxEnvelopeStream());
    }

    @Override
    @GetMapping(value = "/min-max-envelope-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<MinMaxEnvelopeMarketDocuments> minMaxEnvelopeMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<MinMaxEnvelopeMarketDocumentModel> specification = CimSpecification.buildQueryForV1_12(
                permissionId,
                connectionId,
                dataNeedId,
                countryCode,
                regionConnectorId,
                from,
                to
        );
        var all = minMaxRepository.findAll(specification);
        var messages = payloadsOf(all);
        return ResponseEntity.ok()
                             .body(new MinMaxEnvelopeMarketDocuments(messages));
    }


    @Override
    @GetMapping(value = "/energy-sharing-reference-data-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ESRDMDEnvelope>> energySharingReferenceDataMdSSE() {
        return eventStream.toJson(cimConnector.getEnergySharingReferenceDataMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/energy-sharing-reference-data-md", produces = EVENT_STREAM_XML_VALUE)
    public ResponseEntity<Flux<String>> energySharingReferenceDataMdSSEXML() {
        return eventStream.toXml(cimConnector.getEnergySharingReferenceDataMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/energy-sharing-reference-data-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<EnergySharingReferenceDataMarketDocuments> energySharingReferenceDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<EnergySharingReferenceDataMarketDocumentModel> specification = CimSpecification.buildQueryForV1_12(
                permissionId,
                connectionId,
                dataNeedId,
                countryCode,
                regionConnectorId,
                from,
                to
        );
        var all = esrRepository.findAll(specification);
        var messages = payloadsOf(all);
        return ResponseEntity.ok()
                             .body(new EnergySharingReferenceDataMarketDocuments(messages));
    }

    @Override
    @GetMapping(value = "/request-permission-md", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public ResponseEntity<Flux<RequestPermissionEnvelope>> requestPermissionMdSSE() {
        return eventStream.toJson(cimConnector.getRequestPermissionMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/request-permission-md", produces = {EVENT_STREAM_XML_VALUE})
    public ResponseEntity<Flux<String>> requestPermissionMdSSEXML() {
        return eventStream.toXml(cimConnector.getRequestPermissionMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/request-permission-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<RequestPermissionMarketDocuments> requestPermissionMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<RequestPermissionMarketDocumentModel> specification = CimSpecification.buildQueryForV1_12(
                permissionId,
                connectionId,
                dataNeedId,
                countryCode,
                regionConnectorId,
                from,
                to
        );
        var all = requestPermissionMarketDocumentRepository.findAll(specification);
        var messages = payloadsOf(all);
        return ResponseEntity.ok()
                             .body(new RequestPermissionMarketDocuments(messages));
    }
}
