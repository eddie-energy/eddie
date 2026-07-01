// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.agnostic;

import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.cim.agnostic.RawDataMessage;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.dto.ConnectionStatusMessages;
import energy.eddie.outbound.rest.dto.OpaqueEnvelopes;
import energy.eddie.outbound.rest.dto.RawDataMessages;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import energy.eddie.outbound.rest.model.OpaqueEnvelopeModel;
import energy.eddie.outbound.rest.model.RawDataMessageModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.OpaqueEnvelopeRepository;
import energy.eddie.outbound.rest.persistence.RawDataMessageRepository;
import energy.eddie.outbound.rest.persistence.specifications.InsertionTimeSpecification;
import energy.eddie.outbound.rest.persistence.specifications.JsonPathSpecification;
import energy.eddie.outbound.rest.web.EventStream;
import energy.eddie.outbound.shared.TopicStructure;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static energy.eddie.outbound.rest.web.EventStream.EVENT_STREAM_XML_VALUE;
import static org.springframework.http.MediaType.*;

@RestController
@RequestMapping(TopicStructure.AGNOSTIC_VALUE)
public class AgnosticController implements AgnosticSwagger {
    private final AgnosticConnector agnosticConnector;
    private final ConnectionStatusMessageRepository csmRepository;
    private final RawDataMessageRepository rawDataRepository;
    private final OpaqueEnvelopeRepository opaqueEnvelopeRepository;
    private final EventStream eventStream;

    public AgnosticController(
            AgnosticConnector agnosticConnector,
            ConnectionStatusMessageRepository csmRepository,
            RawDataMessageRepository rawDataRepository,
            OpaqueEnvelopeRepository opaqueEnvelopeRepository,
            EventStream eventStream
    ) {
        this.agnosticConnector = agnosticConnector;
        this.csmRepository = csmRepository;
        this.rawDataRepository = rawDataRepository;
        this.opaqueEnvelopeRepository = opaqueEnvelopeRepository;
        this.eventStream = eventStream;
    }

    @Override
    @GetMapping(value = "/connection-status-messages", produces = TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ConnectionStatusMessage>> connectionStatusMessagesSSE() {
        return eventStream.toJson(agnosticConnector.getConnectionStatusMessageStream());
    }

    @Override
    @GetMapping(value = "/connection-status-messages", produces = {EVENT_STREAM_XML_VALUE})
    public ResponseEntity<Flux<String>> connectionStatusMessagesSSEXML() {
        return eventStream.toXml(agnosticConnector.getConnectionStatusMessageStream());
    }

    @Override
    @GetMapping(value = "/connection-status-messages", produces = {APPLICATION_XML_VALUE})
    public ResponseEntity<ConnectionStatusMessages> connectionStatusMessages(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<@Valid @Pattern(regexp = "[A-Z]{2}") String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<ConnectionStatusMessageModel> specification = buildQuery(permissionId,
                                                                                        connectionId,
                                                                                        dataNeedId,
                                                                                        countryCode,
                                                                                        regionConnectorId,
                                                                                        from,
                                                                                        to);
        var all = csmRepository.findAll(specification);
        var messages = ModelWithJsonPayload.payloadsOf(all);
        return ResponseEntity.ok().body(new ConnectionStatusMessages(messages));
    }

    @Override
    @GetMapping(value = "/connection-status-messages", produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<List<ConnectionStatusMessage>> connectionStatusMessagesJson(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<@Valid @Pattern(regexp = "[A-Z]{2}") String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<ConnectionStatusMessageModel> specification = buildQuery(permissionId,
                                                                                        connectionId,
                                                                                        dataNeedId,
                                                                                        countryCode,
                                                                                        regionConnectorId,
                                                                                        from,
                                                                                        to);
        var all = csmRepository.findAll(specification);
        var messages = ModelWithJsonPayload.payloadsOf(all);
        return ResponseEntity.ok().body(messages);
    }

    @Override
    @GetMapping(value = "/raw-data-messages", produces = TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<RawDataMessage>> rawDataMessagesSSE() {
        return eventStream.toJson(agnosticConnector.getRawDataMessageStream());
    }

    @Override
    @GetMapping(value = "/raw-data-messages", produces = EVENT_STREAM_XML_VALUE)
    public ResponseEntity<Flux<String>> rawDataMessagesSSEXML() {
        return eventStream.toXml(agnosticConnector.getRawDataMessageStream());
    }

    @Override
    @GetMapping(value = "/raw-data-messages", produces = {APPLICATION_XML_VALUE})
    public ResponseEntity<RawDataMessages> rawDataMessages(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<RawDataMessageModel> specification = buildQuery(permissionId,
                                                                               connectionId,
                                                                               dataNeedId,
                                                                               countryCode,
                                                                               regionConnectorId,
                                                                               from,
                                                                               to);
        var all = rawDataRepository.findAll(specification);
        var messages = ModelWithJsonPayload.payloadsOf(all);
        return ResponseEntity.ok().body(new RawDataMessages(messages));
    }

    @Override
    @GetMapping(value = "/raw-data-messages", produces = {APPLICATION_JSON_VALUE})
    public ResponseEntity<List<RawDataMessage>> rawDataMessagesJSON(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<RawDataMessageModel> specification = buildQuery(permissionId,
                                                                               connectionId,
                                                                               dataNeedId,
                                                                               countryCode,
                                                                               regionConnectorId,
                                                                               from,
                                                                               to);
        var all = rawDataRepository.findAll(specification);
        var messages = ModelWithJsonPayload.payloadsOf(all);
        return ResponseEntity.ok().body(messages);
    }

    @Override
    @GetMapping(value = "/opaque-envelope", produces = TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<OpaqueEnvelope>> opaqueEnvelopeSSE() {
        return eventStream.toJson(agnosticConnector.getForwardedOpaqueEnvelopeStream());
    }

    @Override
    @GetMapping(value = "/opaque-envelope", produces = EVENT_STREAM_XML_VALUE)
    public ResponseEntity<Flux<String>> opaqueEnvelopeSSEXML() {
        return eventStream.toXml(agnosticConnector.getForwardedOpaqueEnvelopeStream());
    }

    @Override
    @GetMapping(value = "/opaque-envelope", produces = APPLICATION_XML_VALUE)
    public ResponseEntity<OpaqueEnvelopes> opaqueEnvelopes(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<OpaqueEnvelopeModel> specification = buildQuery(permissionId,
                                                                               connectionId,
                                                                               dataNeedId,
                                                                               regionConnectorId,
                                                                               from,
                                                                               to);
        var all = opaqueEnvelopeRepository.findAll(specification);
        var messages = ModelWithJsonPayload.payloadsOf(all);
        return ResponseEntity.ok().body(new OpaqueEnvelopes(messages));
    }

    @Override
    @GetMapping(value = "/opaque-envelope", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OpaqueEnvelope>> opaqueEnvelopesJson(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        PredicateSpecification<OpaqueEnvelopeModel> specification = buildQuery(permissionId,
                                                                               connectionId,
                                                                               dataNeedId,
                                                                               regionConnectorId,
                                                                               from,
                                                                               to);
        var all = opaqueEnvelopeRepository.findAll(specification);
        var messages = ModelWithJsonPayload.payloadsOf(all);
        return ResponseEntity.ok().body(messages);
    }

    @Override
    @PostMapping(value = "permission-command", consumes = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<Void> permissionCommand(@RequestBody PermissionCommand permissionCommand) {
        agnosticConnector.publish(permissionCommand);
        return ResponseEntity.accepted().build();
    }

    @Override
    @PostMapping(value = "opaque-envelope", consumes = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<Void> opaqueEnvelope(@RequestBody OpaqueEnvelope opaqueEnvelope) {
        agnosticConnector.publish(opaqueEnvelope);
        return ResponseEntity.accepted().build();
    }

    private static <T> PredicateSpecification<T> buildQuery(
            Optional<String> permissionId,
            Optional<String> connectionId,
            Optional<String> dataNeedId,
            Optional<String> countryCode,
            Optional<String> regionConnectorId,
            Optional<ZonedDateTime> from,
            Optional<ZonedDateTime> to
    ) {
        var query = List.of(permissionId.map(pid -> new JsonPathSpecification<T>("permissionId", pid)),
                            connectionId.map(cid -> new JsonPathSpecification<T>("connectionId", cid)),
                            dataNeedId.map(did -> new JsonPathSpecification<T>("dataNeedId", did)),
                            countryCode.map(cc -> new JsonPathSpecification<T>(List.of("dataSourceInformation",
                                                                                       "countryCode"), cc)),
                            regionConnectorId.map(rc -> new JsonPathSpecification<T>(List.of("dataSourceInformation",
                                                                                             "regionConnectorId"), rc)),
                            from.map(InsertionTimeSpecification::<T>insertedAfterEquals),
                            to.map(InsertionTimeSpecification::<T>insertedBeforeEquals));
        return PredicateSpecification.allOf(query.stream()
                                                 .filter(Optional::isPresent)
                                                 .map(spec -> (PredicateSpecification<T>) spec.get())
                                                 .toList());
    }

    private static <T> PredicateSpecification<T> buildQuery(
            Optional<String> permissionId,
            Optional<String> connectionId,
            Optional<String> dataNeedId,
            Optional<String> regionConnectorId,
            Optional<ZonedDateTime> from,
            Optional<ZonedDateTime> to
    ) {
        var query = List.of(permissionId.map(pid -> new JsonPathSpecification<T>("permissionId", pid)),
                            connectionId.map(cid -> new JsonPathSpecification<T>("connectionId", cid)),
                            dataNeedId.map(did -> new JsonPathSpecification<T>("dataNeedId", did)),
                            regionConnectorId.map(rc -> new JsonPathSpecification<T>("regionConnectorId", rc)),
                            from.map(InsertionTimeSpecification::<T>insertedAfterEquals),
                            to.map(InsertionTimeSpecification::<T>insertedBeforeEquals));
        return PredicateSpecification.allOf(query.stream()
                                                 .filter(Optional::isPresent)
                                                 .map(spec -> (PredicateSpecification<T>) spec.get())
                                                 .toList());
    }
}
