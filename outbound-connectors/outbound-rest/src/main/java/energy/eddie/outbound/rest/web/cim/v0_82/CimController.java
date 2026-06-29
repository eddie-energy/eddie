// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.cim.v0_82;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import energy.eddie.outbound.rest.dto.AccountingPointDataMarketDocuments;
import energy.eddie.outbound.rest.dto.PermissionMarketDocuments;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocuments;
import energy.eddie.outbound.rest.model.cim.v0_82.AccountingPointDataMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v0_82.PermissionMarketDocumentModel;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.cim.v0_82.AccountingPointDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.PermissionMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.specifications.CimSpecification;
import energy.eddie.outbound.rest.web.SSEEndpoints;
import energy.eddie.outbound.shared.TopicStructure;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.outbound.rest.model.ModelWithJsonPayload.payloadsOf;
import static energy.eddie.outbound.rest.web.SSEEndpoints.EVENT_STREAM_XML_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController
@RequestMapping(TopicStructure.CIM_0_82_VALUE)
public class CimController implements CimSwagger {
    private final CimConnector cimConnector;
    private final ValidatedHistoricalDataMarketDocumentRepository vhdRepository;
    private final PermissionMarketDocumentRepository pmdRepository;
    private final AccountingPointDataMarketDocumentRepository apRepository;
    private final SSEEndpoints sseEndpoints;

    public CimController(
            CimConnector cimConnector,
            ValidatedHistoricalDataMarketDocumentRepository vhdRepository,
            PermissionMarketDocumentRepository pmdRepository,
            AccountingPointDataMarketDocumentRepository apRepository,
            SSEEndpoints sseEndpoints
    ) {
        this.cimConnector = cimConnector;
        this.vhdRepository = vhdRepository;
        this.pmdRepository = pmdRepository;
        this.apRepository = apRepository;
        this.sseEndpoints = sseEndpoints;
    }

    @Override
    @GetMapping(value = "/validated-historical-data-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ValidatedHistoricalDataEnvelope>> validatedHistoricalDataMdSSE() {
        return sseEndpoints.json(cimConnector.getHistoricalDataMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/validated-historical-data-md", produces = EVENT_STREAM_XML_VALUE)
    public ResponseEntity<Flux<String>> validatedHistoricalDataMdSSEXML() {
        return sseEndpoints.xml(cimConnector.getHistoricalDataMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/validated-historical-data-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<ValidatedHistoricalDataMarketDocuments> validatedHistoricalDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<ValidatedHistoricalDataMarketDocumentModel> specification = CimSpecification.buildQueryForV0_82(
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
                             .body(new ValidatedHistoricalDataMarketDocuments(messages));
    }

    @Override
    @GetMapping(value = "/permission-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<PermissionEnvelope>> permissionMdSSE() {
        return sseEndpoints.json(cimConnector.getPermissionMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/permission-md", produces = EVENT_STREAM_XML_VALUE)
    public ResponseEntity<Flux<String>> permissionMdSSEXML() {
        return sseEndpoints.xml(cimConnector.getPermissionMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/permission-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<PermissionMarketDocuments> permissionMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<PermissionMarketDocumentModel> specification = CimSpecification.buildQueryForV0_82(
                permissionId,
                connectionId,
                dataNeedId,
                countryCode,
                regionConnectorId,
                from,
                to
        );
        var all = pmdRepository.findAll(specification);
        var messages = payloadsOf(all);
        return ResponseEntity.ok()
                             .body(new PermissionMarketDocuments(messages));
    }

    @Override
    @GetMapping(value = "/accounting-point-data-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<AccountingPointEnvelope>> accountingPointDataMdSSE() {
        return sseEndpoints.json(cimConnector.getAccountingPointDataMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/accounting-point-data-md", produces = EVENT_STREAM_XML_VALUE)
    public ResponseEntity<Flux<String>> accountingPointDataMdSSEXML() {
        return sseEndpoints.xml(cimConnector.getAccountingPointDataMarketDocumentStream());
    }

    @Override
    @GetMapping(value = "/accounting-point-data-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<AccountingPointDataMarketDocuments> accountingPointDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<AccountingPointDataMarketDocumentModel> specification = CimSpecification.buildQueryForV0_82(
                permissionId,
                connectionId,
                dataNeedId,
                countryCode,
                regionConnectorId,
                from,
                to
        );
        var all = apRepository.findAll(specification);
        var messages = payloadsOf(all);
        return ResponseEntity.ok()
                             .body(new AccountingPointDataMarketDocuments(messages));
    }

    @Override
    @PostMapping(value = "termination-md", consumes = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<Void> terminationMd(@RequestBody PermissionEnvelope permissionEnvelope) {
        cimConnector.publish(permissionEnvelope);
        return ResponseEntity.accepted()
                             .build();
    }
}
