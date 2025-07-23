package energy.eddie.outbound.rest.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.specifications.InsertionTimeSpecification;
import energy.eddie.outbound.rest.persistence.specifications.JsonPathSpecification;
import energy.eddie.outbound.shared.TopicStructure;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.*;

@RestController()
@RequestMapping(TopicStructure.AGNOSTIC_VALUE)
public class AgnosticController {
    private final AgnosticConnector agnosticConnector;
    private final ConnectionStatusMessageRepository repository;

    public AgnosticController(AgnosticConnector agnosticConnector, ConnectionStatusMessageRepository repository) {
        this.agnosticConnector = agnosticConnector;
        this.repository = repository;
    }

    @GetMapping(value = "/connection-status-messages", produces = TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ConnectionStatusMessage>> connectionStatusMessagesSSE() {
        //noinspection UastIncorrectHttpHeaderInspection
        return ResponseEntity.ok()
                             // Tell reverse proxies like Nginx not to buffer the response
                             .header("X-Accel-Buffering", "no")
                             .body(agnosticConnector.getConnectionStatusMessageStream());
    }

    @GetMapping(value = "/connection-status-messages", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<List<ConnectionStatusMessage>> connectionStatusMessages(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        var specification = buildQuery(permissionId,
                                       connectionId,
                                       dataNeedId,
                                       countryCode,
                                       regionConnectorId,
                                       from,
                                       to);
        var all = repository.findAll(specification);
        var messages = new ArrayList<ConnectionStatusMessage>();
        for (var model : all) {
            var payload = model.payload();
            messages.add(payload);
        }
        return ResponseEntity.ok()
                             .body(messages);
    }

    private static Specification<ConnectionStatusMessageModel> buildQuery(
            Optional<String> permissionId,
            Optional<String> connectionId,
            Optional<String> dataNeedId,
            Optional<String> countryCode,
            Optional<String> regionConnectorId,
            Optional<ZonedDateTime> from,
            Optional<ZonedDateTime> to
    ) {
        var query = List.of(
                permissionId.map(pid -> new JsonPathSpecification<ConnectionStatusMessageModel>("permissionId", pid)),
                connectionId.map(cid -> new JsonPathSpecification<ConnectionStatusMessageModel>("connectionId", cid)),
                dataNeedId.map(did -> new JsonPathSpecification<ConnectionStatusMessageModel>("dataNeedId", did)),
                countryCode.map(cc -> new JsonPathSpecification<ConnectionStatusMessageModel>(List.of(
                        "dataSourceInformation",
                        "countryCode"), cc)),
                regionConnectorId.map(rc -> new JsonPathSpecification<ConnectionStatusMessageModel>(List.of(
                        "dataSourceInformation",
                        "regionConnectorId"), rc)),
                from.map(InsertionTimeSpecification::<ConnectionStatusMessageModel>insertedAfterEquals),
                to.map(InsertionTimeSpecification::<ConnectionStatusMessageModel>insertedBeforeEquals)
        );
        return Specification.allOf(
                query.stream()
                     .filter(Optional::isPresent)
                     .map(spec -> (Specification<ConnectionStatusMessageModel>) spec.get())
                     .toList()
        );
    }
}
