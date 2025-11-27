// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.cim.v1_04;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v1_04.CimConnector;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocumentsV1_04;
import energy.eddie.outbound.rest.dto.v1_04.NearRealTimeDataMarketDocuments;
import energy.eddie.outbound.rest.model.cim.v1_04.NearRealTimeDataMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v1_04.ValidatedHistoricalDataMarketDocumentModelV1_04;
import energy.eddie.outbound.rest.persistence.cim.v1_04.NearRealTimeDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v1_04.ValidatedHistoricalDataMarketDocumentV1_04Repository;
import energy.eddie.outbound.rest.persistence.specifications.CimSpecification;
import energy.eddie.outbound.shared.TopicStructure;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.outbound.rest.model.ModelWithJsonPayload.payloadsOf;
import static energy.eddie.outbound.rest.web.cim.v0_82.CimController.X_ACCEL_BUFFERING;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController(value = "cimControllerV1_04")
@RequestMapping(TopicStructure.CIM_1_04_VALUE)
@SuppressWarnings("java:S6830")
// Names shouldn't contain underscores, but this is required to not have bean name clashes with the other CimController
public class CimController implements CimSwagger {
    private final CimConnector cimConnector;
    private final ValidatedHistoricalDataMarketDocumentV1_04Repository vhdRepository;
    private final NearRealTimeDataMarketDocumentRepository rtdRepository;

    public CimController(
            CimConnector cimConnector,
            ValidatedHistoricalDataMarketDocumentV1_04Repository vhdRepository,
            NearRealTimeDataMarketDocumentRepository rtdRepository
    ) {
        this.cimConnector = cimConnector;
        this.vhdRepository = vhdRepository;
        this.rtdRepository = rtdRepository;
    }

    @Override
    @GetMapping(value = "/validated-historical-data-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<VHDEnvelope>> validatedHistoricalDataMdSSE() {
        return ResponseEntity.ok()
                             // Tell reverse proxies like Nginx not to buffer the response
                             .header(X_ACCEL_BUFFERING, "no")
                             .body(cimConnector.getValidatedHistoricalDataMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/validated-historical-data-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<ValidatedHistoricalDataMarketDocumentsV1_04> validatedHistoricalDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<ValidatedHistoricalDataMarketDocumentModelV1_04> specification = CimSpecification.buildQueryForV1_04(
                permissionId,
                connectionId,
                dataNeedId,
                countryCode,
                regionConnectorId,
                from,
                to
        );
        var all = vhdRepository.findAll(specification);
        var messages = payloadsOf(all);
        return ResponseEntity.ok()
                             .body(new ValidatedHistoricalDataMarketDocumentsV1_04(messages));
    }

    @Override
    @GetMapping(value = "/near-real-time-data-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<RTDEnvelope>> nearRealTimeDataMdSSE() {
        return ResponseEntity.ok()
                             // Tell reverse proxies like Nginx not to buffer the response
                             .header(X_ACCEL_BUFFERING, "no")
                             .body(cimConnector.getNearRealTimeDataMarketDocumentStream());
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
        PredicateSpecification<NearRealTimeDataMarketDocumentModel> specification = CimSpecification.buildQueryForV1_04(
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
}
