// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.agnostic;

import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.cim.agnostic.RawDataMessage;
import energy.eddie.outbound.rest.dto.ConnectionStatusMessages;
import energy.eddie.outbound.rest.dto.OpaqueEnvelopes;
import energy.eddie.outbound.rest.dto.RawDataMessages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused", "java:S114"})
@Tag(name = "Agnostic EDDIE Messages", description = "Provides endpoints for non-CIM messages that are EDDIE specific.")
public interface AgnosticSwagger {
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
    ResponseEntity<Flux<ConnectionStatusMessage>> connectionStatusMessagesSSE();


    @Operation(
            operationId = "GET ConnectionStatusMessage stream",
            summary = "Get ConnectionStatusMessage stream",
            description = "Get all new ConnectionStatusMessages as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/vnd.eddie.energy.sse+xml",
                            schema = @Schema(implementation = ConnectionStatusMessage.class)
                    )
            )
    )
    ResponseEntity<Flux<String>> connectionStatusMessagesSSEXML();

    @Operation(
            operationId = "GET ConnectionStatusMessages",
            summary = "Get ConnectionStatusMessages",
            description = "Query available ConnectionStatusMessages",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
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
    ResponseEntity<ConnectionStatusMessages> connectionStatusMessages(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<@jakarta.validation.Valid @jakarta.validation.constraints.Pattern(regexp = "[A-Z]{2}") String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );

    @Operation(
            operationId = "GET ConnectionStatusMessages JSON",
            summary = "Get ConnectionStatusMessages JSON",
            description = "Query available ConnectionStatusMessages JSON",
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
    ResponseEntity<List<ConnectionStatusMessage>> connectionStatusMessagesJson(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<@Valid @Pattern(regexp = "[A-Z]{2}") String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );

    @Operation(
            operationId = "GET RawDataMessage stream",
            summary = "Get RawDataMessage stream",
            description = "Get all new RawDataMessage as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = RawDataMessage.class),
                            examples = @ExampleObject(
                                    // language=JSON
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
                                                "rawPayload": "{}"
                                              }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<RawDataMessage>> rawDataMessagesSSE();

    @Operation(
            operationId = "GET RawDataMessage stream",
            summary = "Get RawDataMessage stream",
            description = "Get all new RawDataMessage as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/vnd.eddie.energy.sse+xml",
                            schema = @Schema(implementation = RawDataMessage.class)
                    )
            )
    )
    ResponseEntity<Flux<String>> rawDataMessagesSSEXML();

    @Operation(
            operationId = "GET RawDataMessages",
            summary = "Get RawDataMessages",
            description = "Query available RawDataMessages",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = RawDataMessage.class)),
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
                                                        "rawPayload": "{}"
                                                      }]
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = RawDataMessages.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                        <RawDataMessages>
                                                           <RawDataMessage>
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
                                                                <rawPayload>{}</rawPayload>
                                                            </RawDataMessage>
                                                        </RawDataMessages>
                                                    """
                                    )
                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by connectionId ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<RawDataMessages> rawDataMessages(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );

    @Operation(
            operationId = "GET RawDataMessages JSON",
            summary = "Get RawDataMessages JSON",
            description = "Query available RawDataMessages as JSON",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RawDataMessage.class)),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                              [{
                                                "connectionId": "1",
                                                "permissionId": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                "dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                "dataSourceInformation": {
                                                  "countryCode": "DE",
                                                  "meteredDataAdministratorId": "sim",
                                                  "permissionAdministratorId": "sim",
                                                  "regionConnectorId": "sim"
                                                },
                                                "timestamp": "2025-07-23T10:31:30.225890564Z",
                                                "rawPayload": "{}"
                                              }]
                                            """
                            )
                    )
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by connectionId ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the RawDataMessages by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<List<RawDataMessage>> rawDataMessagesJSON(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );

    @Operation(
            operationId = "GET OpaqueEnvelope stream",
            summary = "Get OpaqueEnvelope stream",
            description = "Get all new OpaqueEnvelopes as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = OpaqueEnvelope.class),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                              {
                                                "connectionId": "1",
                                                "permissionId": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                "dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                "regionConnectorId": "aiida",
                                                "payload": "{}"
                                              }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<OpaqueEnvelope>> opaqueEnvelopeSSE();

    @Operation(
            operationId = "GET OpaqueEnvelope stream",
            summary = "Get OpaqueEnvelope stream",
            description = "Get all new OpaqueEnvelopes as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/vnd.eddie.energy.sse+xml",
                            schema = @Schema(implementation = OpaqueEnvelope.class)
                    )
            )
    )
    ResponseEntity<Flux<String>> opaqueEnvelopeSSEXML();

    @Operation(
            operationId = "GET OpaqueEnvelopes",
            summary = "Get OpaqueEnvelopes",
            description = "Query available OpaqueEnvelopes forwarded to the eligible party",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = OpaqueEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                      [{
                                                        "connectionId": "1",
                                                        "permissionId": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                        "dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                        "regionConnectorId": "aiida",
                                                        "payload": "{}"
                                                      }]
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = OpaqueEnvelopes.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                        <OpaqueEnvelopes>
                                                            <OpaqueEnvelope>
                                                                <connectionId>1</connectionId>
                                                                <permissionId>ffcb8491-1f82-4d9d-9ddf-f1312796045a</permissionId>
                                                                <dataNeedId>9bd0668f-cc19-40a8-99db-dc2cb2802b17</dataNeedId>
                                                                <regionConnectorId>aiida</regionConnectorId>
                                                                <payload>{}</payload>
                                                            </OpaqueEnvelope>
                                                        </OpaqueEnvelopes>
                                                    """
                                    )
                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by connectionId ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<OpaqueEnvelopes> opaqueEnvelopes(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );

    @Operation(
            operationId = "GET OpaqueEnvelopes JSON",
            summary = "Get OpaqueEnvelopes JSON",
            description = "Query available OpaqueEnvelopes forwarded to the eligible party as JSON",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OpaqueEnvelope.class)),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                              [{
                                                "connectionId": "1",
                                                "permissionId": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                "dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                "regionConnectorId": "aiida",
                                                "payload": "{}"
                                              }]
                                            """
                            )
                    )
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by connectionId ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the OpaqueEnvelopes by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<List<OpaqueEnvelope>> opaqueEnvelopesJson(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );

    @Operation(
            operationId = "POST permission command",
            summary = "POST permission command",
            description = "POST a permission command, that will be forwarded to the region connectors",
            method = "POST",
            responses = @ApiResponse(responseCode = "202"),
            requestBody = @RequestBody(
                    description = "The permission command, which contains control commands for permissions, that will be forwarded to the region connectors",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PermissionCommand.class),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    {
                                                      "regionConnectorId": "aiida",
                                                      "permissionId": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                      "action": "UPDATE_TRANSMISSION_SCHEDULE",
                                                      "transmissionSchedule": "0 */1 * * * *"
                                                    }
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = PermissionCommand.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <PermissionCommand>
                                                      <regionConnectorId>aiida</regionConnectorId>
                                                      <permissionId>ffcb8491-1f82-4d9d-9ddf-f1312796045a</permissionId>
                                                      <action>UPDATE_TRANSMISSION_SCHEDULE</action>
                                                      <transmissionSchedule>0 */1 * * * *</transmissionSchedule>
                                                    </PermissionCommand>
                                                    """
                                    )
                            )
                    }
            )
    )
    ResponseEntity<Void> permissionCommand(PermissionCommand permissionCommand);

    @Operation(
            operationId = "POST opaque envelope",
            summary = "POST opaque envelope",
            description = "POST a opaque envelope, that will be forwarded to the region connectors",
            method = "POST",
            responses = @ApiResponse(responseCode = "202"),
            requestBody = @RequestBody(
                    description = "The opaque envelope, which contains any payload and metadata, that will be forwarded to the region connectors",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = OpaqueEnvelope.class),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    {
                                                      "connectionId": "1",
                                                      "permissionId": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                      "dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                      "regionConnectorId": "aiida",
                                                      "payload": "{}"
                                                    }
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = OpaqueEnvelope.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <OpaqueEnvelope>
                                                      <connectionId>1</connectionId>
                                                      <permissionId>ffcb8491-1f82-4d9d-9ddf-f1312796045a</permissionId>
                                                      <dataNeedId>9bd0668f-cc19-40a8-99db-dc2cb2802b17</dataNeedId>
                                                      <regionConnectorId>aiida</regionConnectorId>
                                                      <payload>{}</payload>
                                                    </OpaqueEnvelope>
                                                    """
                                    )
                            )
                    }
            )
    )
    ResponseEntity<Void> opaqueEnvelope(OpaqueEnvelope opaqueEnvelope);
}
