package energy.eddie.outbound.rest.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.dto.ConnectionStatusMessages;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import energy.eddie.outbound.rest.persistence.specifications.InsertionTimeSpecification;
import energy.eddie.outbound.rest.persistence.specifications.JsonPathSpecification;
import energy.eddie.outbound.shared.TopicStructure;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.MediaType.*;

@RestController
@RequestMapping(TopicStructure.AGNOSTIC_VALUE)
@Tag(name = "Agnostic EDDIE Messages", description = "Provides endpoints for non-CIM messages that are EDDIE specific.")
public class AgnosticController {
    private final AgnosticConnector agnosticConnector;
    private final ConnectionStatusMessageRepository repository;

    public AgnosticController(AgnosticConnector agnosticConnector, ConnectionStatusMessageRepository repository) {
        this.agnosticConnector = agnosticConnector;
        this.repository = repository;
    }

    @Operation(
            operationId = "GET ConnectionStatusMessage stream",
            summary = "Get ConnectionStatusMessage stream",
            description = "Get all new ConnectionStatusMessages as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = ConnectionStatusMessage.class),
                            examples = @ExampleObject(
                                    value = """
                                              {
                                                "connectionId": "1",
                                                "permissionId": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                "dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                "dataSourceInformation": {
                                                  "countryCode": "AT°",
                                                  "meteredDataAdministratorId": "sim",
                                                  "permissionAdministratorId": "sim",
                                                  "regionConnectorId": "sim"
                                                },
                                                "timestamp": "2025-07-23T10:31:30.225890564Z",
                                                "status": "FULFILLED",
                                                "message": "Permission request is fulfilled",
                                                "additionalInformation": null
                                              }
                                            """
                            )
                    )
            )
    )
    @GetMapping(value = "/connection-status-messages", produces = TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ConnectionStatusMessage>> connectionStatusMessagesSSE() {
        //noinspection UastIncorrectHttpHeaderInspection
        return ResponseEntity.ok()
                             // Tell reverse proxies like Nginx not to buffer the response
                             .header("X-Accel-Buffering", "no")
                             .body(agnosticConnector.getConnectionStatusMessageStream());
    }

    @Operation(
            operationId = "GET ConnectionStatusMessages",
            summary = "Get ConnectionStatusMessages",
            description = "Query available ConnectionStatusMessages",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ConnectionStatusMessage.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                      [{
                                                        "connectionId": "1",
                                                        "permissionId": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                        "dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                        "dataSourceInformation": {
                                                          "countryCode": "DE°",
                                                          "meteredDataAdministratorId": "sim",
                                                          "permissionAdministratorId": "sim",
                                                          "regionConnectorId": "sim"
                                                        },
                                                        "timestamp": "2025-07-23T10:31:30.225890564Z",
                                                        "status": "FULFILLED",
                                                        "message": "Permission request is fulfilled",
                                                        "additionalInformation": null
                                                      }]
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = ConnectionStatusMessages.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                        <ConnectionStatusMessages>
                                                           <ConnectionStatusMessage>
                                                                <connectionId>1</connectionId>
                                                                <permissionId>ffcb8491-1f82-4d9d-9ddf-f1312796045a</permissionId>
                                                                <dataNeedId>9bd0668f-cc19-40a8-99db-dc2cb2802b17</dataNeedId>
                                                                <dataSourceInformation>
                                                                    <countryCode>DE</countryCode>
                                                                    <meteredDataAdministratorId>sim</meteredDataAdministratorId>
                                                                    <permissionAdministratorId>sim</permissionAdministratorId>
                                                                    <regionConnectorId>sim</regionConnectorId>
                                                                </dataSourceInformation>
                                                                <timestamp>2025-07-23T10:31:30.225890564Z</timestamp>
                                                                <status>FULFILLED</status>
                                                                <message>Permission request is fulfilled</message>
                                                                <additionalInformation/>
                                                            </ConnectionStatusMessage>
                                                        </ConnectionStatusMessages>
                                                    """
                                    )
                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the ConnectionStatusMessages by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the ConnectionStatusMessages by connectionId ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the ConnectionStatusMessages by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the ConnectionStatusMessages by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the ConnectionStatusMessages by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the ConnectionStatusMessages by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the ConnectionStatusMessages by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    @GetMapping(
            value = "/connection-status-messages",
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE}
    )
    public ResponseEntity<ConnectionStatusMessages> connectionStatusMessages(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<@Valid @Pattern(regexp = "[A-Z]{2}") String> countryCode,
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
        var messages = ModelWithJsonPayload.payloadsOf(all);
        return ResponseEntity.ok()
                             .body(new ConnectionStatusMessages(messages));
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
