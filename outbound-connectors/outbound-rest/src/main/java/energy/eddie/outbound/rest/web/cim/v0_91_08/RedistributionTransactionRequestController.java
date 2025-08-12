package energy.eddie.outbound.rest.web.cim.v0_91_08;

import energy.eddie.api.agnostic.retransmission.result.*;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.rest.connectors.RestRetransmissionConnector;
import energy.eddie.outbound.shared.TopicStructure;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController
@RequestMapping(TopicStructure.CIM_0_91_08_VALUE)
@Tag(name = "Redistribution Transaction Request Documents", description = "Provides endpoints to create redistribution request documents.")
public class RedistributionTransactionRequestController {
    private final RestRetransmissionConnector retransmissionConnector;

    public RedistributionTransactionRequestController(RestRetransmissionConnector retransmissionConnector) {this.retransmissionConnector = retransmissionConnector;}

    @Operation(
            operationId = "POST Redistribution Transaction Request",
            summary = "POST Redistribution Transaction Request",
            description = "Create a new Redistribution Transaction Request for a specific region connector and permission request",
            externalDocs = @ExternalDocumentation(url = "https://eddie-web.projekte.fh-hagenberg.at/framework/2-integrating/messages/cim/redistribution-transaction-request-documents.html"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    {
                                                      "messageDocumentHeader.creationDateTime": "2025-01-03T00:00:00Z",
                                                      "messageDocumentHeader.metaInformation.permissionId": "permissionId",
                                                      "messageDocumentHeader.metaInformation.region.connector": "rc-id",
                                                      "marketDocument.period.timeInterval": {
                                                        "start": "2025-01-01T01:01Z",
                                                        "end": "2025-01-02T00:00Z"
                                                      }
                                                    }
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                                                    <ns4:RTR_Envelope
                                                            xmlns:ns4="https://eddie.energy/CIM/RTR">
                                                        <ns4:messageDocumentHeader.creationDateTime>2025-01-03T00:00:00Z</ns4:messageDocumentHeader.creationDateTime>
                                                        <ns4:messageDocumentHeader.metaInformation.permissionId>1aa0ef01-98c3-4e5f-be51-af3d0ccbfffc</ns4:messageDocumentHeader.metaInformation.permissionId>
                                                        <ns4:messageDocumentHeader.metaInformation.region.connector>cds</ns4:messageDocumentHeader.metaInformation.region.connector>
                                                        <ns4:marketDocument.period.timeInterval>
                                                            <ns4:start>2025-01-01T01:01Z</ns4:start>
                                                            <ns4:end>2025-01-02T00:00Z</ns4:end>
                                                        </ns4:marketDocument.period.timeInterval>
                                                    </ns4:RTR_Envelope>
                                                    """
                                    )
                            )
                    }
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The redistribution transaction request was created successfully results will be available on the validated-historical-data-md endpoint"
                    ),
                    @ApiResponse(
                            responseCode = "204",
                            description = "The request was correct, but the data is not available to the region connector"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Failure creating the request"
                    ),
                    @ApiResponse(
                            responseCode = "410",
                            description = "The permission request is not active anymore and the data cannot be requested"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "The permission request exists, but does not include the timeframe for the requested data or does not support validated historical data"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Either the region connector or permission request do not exist"
                    ),
            }
    )
    @PostMapping(
            value = "/redistribution-transaction-rd",
            consumes = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE}
    )
    public Mono<ResponseEntity<Void>> redistributionTransactionRd(@RequestBody RTREnvelope rtrEnvelope) {
        return retransmissionConnector.publish(rtrEnvelope)
                                      .map(this::getResponseEntity);
    }

    private ResponseEntity<Void> getResponseEntity(RetransmissionResult res) {
        return switch (res) {
            case DataNotAvailable ignored -> ResponseEntity.noContent().build();
            case Failure ignored -> ResponseEntity.internalServerError().build();
            case NoActivePermission ignored -> ResponseEntity.status(HttpStatus.GONE).build();
            case NoPermissionForTimeFrame ignored -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case NotSupported ignored -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case PermissionRequestNotFound ignored -> ResponseEntity.notFound().build();
            case RetransmissionServiceNotFound ignored -> ResponseEntity.notFound().build();
            case Success ignored -> ResponseEntity.ok().build();
        };
    }
}
