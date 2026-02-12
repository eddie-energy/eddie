package energy.eddie.outbound.rest.web.cim.v1_12;

import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.dto.v1_12.NearRealTimeDataMarketDocuments;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused", "java:S114"})
@Tag(name = "CIM v1.12 Documents", description = "Provides endpoints for CIM v1.12 documents, such as validated historical data market documents.")
public interface CimSwagger {
    @Operation(
            operationId = "GET near real-time data market document stream",
            summary = "GET near real-time data market document stream",
            description = "Get all new near real-time data market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = RTDEnvelope.class),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                            {
                                              "MessageDocumentHeader": {
                                                "creationDateTime": "2026-02-11T15:32:25Z",
                                                "MetaInformation": {
                                                  "connectionId": "1",
                                                  "requestPermissionId": "70744400-a059-4fc8-ab36-d68b2bb877e1",
                                                  "dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
                                                  "documentType": "near-real-time-market-document",
                                                  "finalCustomerId": "88e0fc2c-4ea7-4850-a736-8b9742757518",
                                                  "dataSourceId": "7d2b2547-27dd-4fe0-9516-707540e1184f",
                                                  "regionConnector": "aiida",
                                                  "regionCountry": "AT",
                                                  "Asset": {
                                                    "type": "CONNECTION-AGREEMENT-POINT",
                                                    "operatorId": "AT003000",
                                                    "meterId": "003114735"
                                                  }
                                                }
                                              },
                                              "MarketDocument": {
                                                "mRID": "bfc16eda-4f05-4711-b319-af17ec0ce6d5",
                                                "createdDateTime": "2026-02-11T15:32:25Z",
                                                "TimeSeries": [
                                                  {
                                                    "version": "1.0",
                                                    "dateAndOrTime.dateTime": "2026-02-11T15:32:24Z",
                                                    "Quantity": [
                                                      {
                                                        "quantity": 0.132,
                                                        "type": "2",
                                                        "quality": "AS_PROVIDED"
                                                      },
                                                      {
                                                        "quantity": 65238.377,
                                                        "type": "0",
                                                        "quality": "AS_PROVIDED"
                                                      }
                                                    ],
                                                    "registeredResource.mRID": {
                                                      "value": "7d2b2547-27dd-4fe0-9516-707540e1184f",
                                                      "codingScheme": "NAT"
                                                    }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<RTDEnvelope>> nearRealTimeDataMdSSE();


    @Operation(
            operationId = "GET near real-time data market documents",
            summary = "GET near real-time data market documents",
            description = "Get all past near real-time data market documents",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = RTDEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    [
                                                      {
                                                        "MessageDocumentHeader": {
                                                          "creationDateTime": "2026-02-11T15:32:25Z",
                                                          "MetaInformation": {
                                                            "connectionId": "1",
                                                            "requestPermissionId": "70744400-a059-4fc8-ab36-d68b2bb877e1",
                                                            "dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
                                                            "documentType": "near-real-time-market-document",
                                                            "finalCustomerId": "88e0fc2c-4ea7-4850-a736-8b9742757518",
                                                            "dataSourceId": "7d2b2547-27dd-4fe0-9516-707540e1184f",
                                                            "regionConnector": "aiida",
                                                            "regionCountry": "AT",
                                                            "Asset": {
                                                              "type": "CONNECTION-AGREEMENT-POINT",
                                                              "operatorId": "AT003000",
                                                              "meterId": "003114735"
                                                            }
                                                          }
                                                        },
                                                        "MarketDocument": {
                                                          "mRID": "bfc16eda-4f05-4711-b319-af17ec0ce6d5",
                                                          "createdDateTime": "2026-02-11T15:32:25Z",
                                                          "TimeSeries": [
                                                            {
                                                              "version": "1.0",
                                                              "dateAndOrTime.dateTime": "2026-02-11T15:32:24Z",
                                                              "Quantity": [
                                                                {
                                                                  "quantity": 0.132,
                                                                  "type": "2",
                                                                  "quality": "AS_PROVIDED"
                                                                },
                                                                {
                                                                  "quantity": 65238.377,
                                                                  "type": "0",
                                                                  "quality": "AS_PROVIDED"
                                                                }
                                                              ],
                                                              "registeredResource.mRID": {
                                                                "value": "7d2b2547-27dd-4fe0-9516-707540e1184f",
                                                                "codingScheme": "NAT"
                                                              }
                                                            }
                                                          ]
                                                        }
                                                      }
                                                    ]
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = NearRealTimeDataMarketDocuments.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <RTD_Envelope xmlns="https//eddie.energy/CIM/RTD_v1.12">
                                                        <MessageDocumentHeader>
                                                            <creationDateTime>2026-02-12T08:03:40Z</creationDateTime>
                                                            <MetaInformation>
                                                                <connectionId>1</connectionId>
                                                                <requestPermissionId>aae63ff1-4062-4599-8f4c-686df39138e7</requestPermissionId>
                                                                <dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a84</dataNeedId>
                                                                <documentType>near-real-time-market-document</documentType>
                                                                <finalCustomerId>88e0fc2c-4ea7-4850-a736-8b9742757518</finalCustomerId>
                                                                <dataSourceId>0743c9d8-3e5f-4575-999b-34f6f83b2075</dataSourceId>
                                                                <regionConnector>aiida</regionConnector>
                                                                <regionCountry>AT</regionCountry>
                                                                <Asset>
                                                                    <type>CONNECTION-AGREEMENT-POINT</type>
                                                                   <operatorId>AT003000</operatorId>
                                                                   <meterId>003114735</meterId>
                                                                </Asset>
                                                            </MetaInformation>
                                                        </MessageDocumentHeader>
                                                        <MarketDocument>
                                                            <mRID>78f93c55-c666-43b3-bbf2-a07059cad002</mRID>
                                                            <createdDateTime>2026-02-12T08:03:40Z</createdDateTime>
                                                            <TimeSeries>
                                                                <TimeSeries>
                                                                    <version>1.0</version>
                                                                    <dateAndOrTime.dateTime>2026-02-12T08:03:38Z</dateAndOrTime.dateTime>
                                                                    <Quantity>
                                                                        <Quantity>
                                                                            <quantity>0.117</quantity>
                                                                            <type>2</type>
                                                                            <quality>AS_PROVIDED</quality>
                                                                        </Quantity>
                                                                        <Quantity>
                                                                            <quantity>65238.377</quantity>
                                                                            <type>0</type>
                                                                            <quality>AS_PROVIDED</quality>
                                                                        </Quantity>
                                                                    </Quantity>
                                                                    <registeredResource.mRID codingScheme="NAT">
                                                                        0743c9d8-3e5f-4575-999b-34f6f83b2075
                                                                    </registeredResource.mRID>
                                                                </TimeSeries>
                                                            </TimeSeries>
                                                        </MarketDocument>
                                                    </RTD_Envelope>
                                                    """
                                    )

                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the near real-time data market documents by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the near real-time data market documents by connectionId ID",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the near real-time data market documents by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the near real-time data market documents by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "N[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the near real-time data market documents by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the near real-time data market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the near real-time data market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<NearRealTimeDataMarketDocuments> nearRealTimeDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );
}
