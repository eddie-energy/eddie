// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.cim.v1_12;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.dto.v1_12.*;
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

    @Operation(
            operationId = "GET acknowledgement market document stream",
            summary = "GET acknowledgement market document stream",
            description = "Get all new acknowledgement market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = AcknowledgementEnvelope.class),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                            {
                                              "MessageDocumentHeader": {
                                                "creationDateTime": "2026-02-24T11:58:05Z",
                                                "MetaInformation": {
                                                  "connectionId": "1",
                                                  "requestPermissionId": "aae63ff1-4062-4599-8f4c-686df39138e7",
                                                  "dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
                                                  "documentType": "acknowledgement-market-document",
                                                  "finalCustomerId": "88e0fc2c-4ea7-4850-a736-8b9742757518",
                                                  "dataSourceId": "0743c9d8-3e5f-4575-999b-34f6f83b2075",
                                                  "defaultValues": null,
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
                                                "mRID": "1cb72828-e869-463c-a447-7062bcca24b4",
                                                "createdDateTime": "2026-02-24T11:58:05Z",
                                                "sender_MarketParticipant.mRID": {
                                                  "value": "AT003000",
                                                  "codingScheme": "NAT"
                                                },
                                                "sender_MarketParticipant.marketRole.type": "CONNECTING_SYSTEM_OPERATOR",
                                                "receiver_MarketParticipant.mRID": {
                                                  "value": "88e0fc2c-4ea7-4850-a736-8b9742757518",
                                                  "codingScheme": "NAT"
                                                },
                                                "receiver_MarketParticipant.marketRole.type": "FINAL_CUSTOMER",
                                                "received_MarketDocument.mRID": "5a1cd7e9-345a-4c5a-94e3-987b3b8d2def",
                                                "received_MarketDocument.revisionNumber": "1",
                                                "received_MarketDocument.type": "min-max-envelope",
                                                "received_MarketDocument.process.processType": "MIN_MAX_ENVELOPE",
                                                "received_MarketDocument.title": null,
                                                "received_MarketDocument.createdDateTime": "2026-02-24T11:57:05Z",
                                                "InError_Period": null,
                                                "Reason": null,
                                                "Rejected_TimeSeries": [
                                                  {
                                                    "mRID": "63c99336-d854-4ed2-be01-13dca22a2850",
                                                    "version": "1",
                                                    "InError_Period": [
                                                      {
                                                        "Reason": null,
                                                        "timeInterval": {
                                                          "start": "2026-02-24T12:58:05Z",
                                                          "end": "2026-02-24T13:58:05Z"
                                                        }
                                                      }
                                                    ],
                                                    "Reason": [
                                                      {
                                                        "code": "A01",
                                                        "text": "Invalid time series data"
                                                      }
                                                    ]
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<AcknowledgementEnvelope>> acknowledgementMdSSE();


    @Operation(
            operationId = "GET acknowledgement market documents",
            summary = "GET acknowledgement market documents",
            description = "Get all past acknowledgement market documents",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AcknowledgementEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    [
                                                      {
                                                        "MessageDocumentHeader": {
                                                          "creationDateTime": "2026-02-24T11:58:05Z",
                                                          "MetaInformation": {
                                                            "connectionId": "1",
                                                            "requestPermissionId": "aae63ff1-4062-4599-8f4c-686df39138e7",
                                                            "dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
                                                            "documentType": "acknowledgement-market-document",
                                                            "finalCustomerId": "88e0fc2c-4ea7-4850-a736-8b9742757518",
                                                            "dataSourceId": "0743c9d8-3e5f-4575-999b-34f6f83b2075",
                                                            "defaultValues": null,
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
                                                          "mRID": "1cb72828-e869-463c-a447-7062bcca24b4",
                                                          "createdDateTime": "2026-02-24T11:58:05Z",
                                                          "sender_MarketParticipant.mRID": {
                                                            "value": "AT003000",
                                                            "codingScheme": "NAT"
                                                          },
                                                          "sender_MarketParticipant.marketRole.type": "CONNECTING_SYSTEM_OPERATOR",
                                                          "receiver_MarketParticipant.mRID": {
                                                            "value": "88e0fc2c-4ea7-4850-a736-8b9742757518",
                                                            "codingScheme": "NAT"
                                                          },
                                                          "receiver_MarketParticipant.marketRole.type": "FINAL_CUSTOMER",
                                                          "received_MarketDocument.mRID": "5a1cd7e9-345a-4c5a-94e3-987b3b8d2def",
                                                          "received_MarketDocument.revisionNumber": "1",
                                                          "received_MarketDocument.type": "min-max-envelope",
                                                          "received_MarketDocument.process.processType": "MIN_MAX_ENVELOPE",
                                                          "received_MarketDocument.title": null,
                                                          "received_MarketDocument.createdDateTime": "2026-02-24T11:57:05Z",
                                                          "InError_Period": null,
                                                          "Reason": null,
                                                          "Rejected_TimeSeries": [
                                                            {
                                                              "mRID": "63c99336-d854-4ed2-be01-13dca22a2850",
                                                              "version": "1",
                                                              "InError_Period": [
                                                                {
                                                                  "Reason": null,
                                                                  "timeInterval": {
                                                                    "start": "2026-02-24T12:58:05Z",
                                                                    "end": "2026-02-24T13:58:05Z"
                                                                  }
                                                                }
                                                              ],
                                                              "Reason": [
                                                                {
                                                                  "code": "A01",
                                                                  "text": "Invalid time series data"
                                                                }
                                                              ]
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
                                    schema = @Schema(implementation = AcknowledgementEnvelope.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <Acknowledgement_Envelope xmlns="https://eddie.energy/CIM/ACK_v1.12">
                                                        <MessageDocumentHeader>
                                                            <creationDateTime>2026-02-24T11:58:05Z</creationDateTime>
                                                            <MetaInformation>
                                                                <connectionId>1</connectionId>
                                                                <requestPermissionId>aae63ff1-4062-4599-8f4c-686df39138e7</requestPermissionId>
                                                                <dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a84</dataNeedId>
                                                                <documentType>acknowledgement-market-document</documentType>
                                                                <finalCustomerId>88e0fc2c-4ea7-4850-a736-8b9742757518</finalCustomerId>
                                                                <dataSourceId>0743c9d8-3e5f-4575-999b-34f6f83b2075</dataSourceId>
                                                                <defaultValues xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
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
                                                            <mRID>1cb72828-e869-463c-a447-7062bcca24b4</mRID>
                                                            <createdDateTime>2026-02-24T11:58:05Z</createdDateTime>
                                                            <sender_MarketParticipant.mRID codingScheme="NAT">AT003000</sender_MarketParticipant.mRID>
                                                            <sender_MarketParticipant.marketRole.type>CONNECTING_SYSTEM_OPERATOR</sender_MarketParticipant.marketRole.type>
                                                            <receiver_MarketParticipant.mRID codingScheme="NAT">88e0fc2c-4ea7-4850-a736-8b9742757518
                                                            </receiver_MarketParticipant.mRID>
                                                            <receiver_MarketParticipant.marketRole.type>FINAL_CUSTOMER</receiver_MarketParticipant.marketRole.type>
                                                            <received_MarketDocument.mRID>5a1cd7e9-345a-4c5a-94e3-987b3b8d2def</received_MarketDocument.mRID>
                                                            <received_MarketDocument.revisionNumber>1</received_MarketDocument.revisionNumber>
                                                            <received_MarketDocument.type>min-max-envelope</received_MarketDocument.type>
                                                            <received_MarketDocument.process.processType>MIN_MAX_ENVELOPE</received_MarketDocument.process.processType>
                                                            <received_MarketDocument.title xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                                                            <received_MarketDocument.createdDateTime>2026-02-24T11:57:05Z</received_MarketDocument.createdDateTime>
                                                            <Rejected_TimeSeries>
                                                                    <mRID>63c99336-d854-4ed2-be01-13dca22a2850</mRID>
                                                                    <version>1</version>
                                                                    <InError_Period>
                                                                        <timeInterval>
                                                                            <start>2026-02-24T12:58:05Z</start>
                                                                            <end>2026-02-24T13:58:05Z</end>
                                                                        </timeInterval>
                                                                    </InError_Period>
                                                                    <Reason>
                                                                        <code>A01</code>
                                                                        <text>Invalid time series data</text>
                                                                    </Reason>
                                                            </Rejected_TimeSeries>
                                                        </MarketDocument>
                                                    </Acknowledgement_Envelope>
                                                    """
                                    )

                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the acknowledgement market documents by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the acknowledgement market documents by connectionId ID",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the acknowledgement market documents by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the acknowledgement market documents by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "N[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the acknowledgement market documents by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the acknowledgement market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the acknowledgement market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<AcknowledgementMarketDocuments> acknowledgementMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );

    @Operation(
            operationId = "POST min-max envelope market document",
            summary = "POST min-max envelope market document stream",
            description = "POST a min-max envelope market document, that will be forwarded to the region connectors",
            method = "POST",
            responses = @ApiResponse(responseCode = "202"),
            requestBody = @RequestBody(
                    description = "The min-max envelope market document, which contains the minimum and maximum values for a certain quantity, that will be forwarded to the region connectors",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = RECMMOEEnvelope.class),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    {
                                                      "MessageDocumentHeader": {
                                                        "creationDateTime": "2026-02-16T10:11:58Z",
                                                        "MetaInformation": {
                                                          "connectionId": "1",
                                                          "requestPermissionId": "aae63ff1-4062-4599-8f4c-686df39138e7",
                                                          "dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a12",
                                                          "documentType": "min-max-envelope",
                                                          "finalCustomerId": "88e0fc2c-4ea7-4850-a736-8b9742757518",
                                                          "dataSourceId": "0743c9d8-3e5f-4575-999b-34f6f83b2075",
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
                                                        "mRID": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a12",
                                                        "description": "Test Min-Max Envelope",
                                                        "revisionNumber": "1",
                                                        "lastModifiedDateTime": "2026-02-16T10:11:58Z",
                                                        "comment": "This is a test min-max envelope.",
                                                        "sender_MarketParticipant.mRID": {
                                                          "value": "AT003000",
                                                          "codingScheme": "NAT"
                                                        },
                                                        "sender_MarketParticipant.name": "Netz Oberösterreich GmbH",
                                                        "sender_MarketParticipant.marketRole.type": "A56",
                                                        "receiver_MarketParticipant.mRID": {
                                                          "value": "88e0fc2c-4ea7-4850-a736-8b9742757518",
                                                          "codingScheme": "NAT"
                                                        },
                                                        "receiver_MarketParticipant.name": "Max Mustermann",
                                                        "receiver_MarketParticipant.marketRole.type": "A13",
                                                        "process.processType": "A14",
                                                        "period.timeInterval": {
                                                          "start": "2026-06-01T00:00:00Z",
                                                          "end": "2026-06-02T23:59:59Z"
                                                        },
                                                        "TimeSeries_Series": [
                                                          {
                                                            "mRID": "series-1",
                                                            "businessType": "C76",
                                                            "curveType": "A01",
                                                            "resourceTimeSeries.value1ScheduleType": "loadReduction",
                                                            "flowDirection.direction": "A02",
                                                            "registeredResource.mRID": {
                                                              "value": "003114735",
                                                              "codingScheme": "NAT"
                                                            },
                                                            "registeredResource.name": "Test Connection Point",
                                                            "registeredResource.description": "This is a test connection point for the min-max envelope.",
                                                            "Series": [
                                                              {
                                                                "Period": [
                                                                  {
                                                                    "resolution": "P1D",
                                                                    "timeInterval": {
                                                                      "start": "2026-06-01T00:00:00Z",
                                                                      "end": "2026-06-02T23:59:59Z"
                                                                    },
                                                                    "Point": [
                                                                      {
                                                                        "position": 1,
                                                                        "min_Quantity.quantity": 1,
                                                                        "min_Quantity.quality": "1",
                                                                        "max_Quantity.quantity": 4,
                                                                        "max_Quantity.quality": "3"
                                                                      }
                                                                    ]
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = RECMMOEEnvelope.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <RECMMOE_Envelope xmlns="https//eddie.energy/CIM/RECMMOE_v1.12">
                                                        <MessageDocumentHeader>
                                                            <creationDateTime>2026-02-16T10:17:11Z</creationDateTime>
                                                            <MetaInformation>
                                                                <connectionId>1</connectionId>
                                                                <requestPermissionId>aae63ff1-4062-4599-8f4c-686df39138e7</requestPermissionId>
                                                                <dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a12</dataNeedId>
                                                                <documentType>min-max-envelope</documentType>
                                                                <finalCustomerId>fc-id</finalCustomerId>
                                                                <dataSourceId>0743c9d8-3e5f-4575-999b-34f6f83b2075</dataSourceId>
                                                                <defaultValues>minPower=value</defaultValues>
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
                                                            <mRID>5dc71d7e-e8cd-4403-a3a8-d3c095c97a12</mRID>
                                                            <description>Test Min-Max Envelope</description>
                                                            <revisionNumber>1</revisionNumber>
                                                            <lastModifiedDateTime>2026-02-16T10:17:11Z</lastModifiedDateTime>
                                                            <comment>This is a test min-max envelope.</comment>
                                                            <sender_MarketParticipant.mRID codingScheme="NAT">AT003000</sender_MarketParticipant.mRID>
                                                            <sender_MarketParticipant.name>Netz Oberösterreich GmbH</sender_MarketParticipant.name>
                                                            <sender_MarketParticipant.marketRole.type>A56</sender_MarketParticipant.marketRole.type>
                                                            <receiver_MarketParticipant.mRID codingScheme="NAT">fc-id</receiver_MarketParticipant.mRID>
                                                            <receiver_MarketParticipant.name>Max Mustermann</receiver_MarketParticipant.name>
                                                            <receiver_MarketParticipant.marketRole.type>A13</receiver_MarketParticipant.marketRole.type>
                                                            <process.processType>A14</process.processType>
                                                            <period.timeInterval>
                                                                <start>2026-06-01T00:00Z</start>
                                                                <end>2026-06-30T23:59Z</end>
                                                            </period.timeInterval>
                                                            <TimeSeries_Series>
                                                                <mRID>series-1</mRID>
                                                                <businessType>C76</businessType>
                                                                <curveType>A01</curveType>
                                                                <resourceTimeSeries.value1ScheduleType>loadReduction</resourceTimeSeries.value1ScheduleType>
                                                                <flowDirection.direction>A02</flowDirection.direction>
                                                                <registeredResource.mRID codingScheme="NAT">003114735</registeredResource.mRID>
                                                                <registeredResource.name>Test Connection Point</registeredResource.name>
                                                                <registeredResource.description>This is a test connection point for the min-max envelope.
                                                                </registeredResource.description>
                                                                <Series>
                                                                    <Period>
                                                                        <resolution>PT15M</resolution>
                                                                        <timeInterval>
                                                                            <start>2026-06-01T00:00Z</start>
                                                                            <end>2026-06-30T23:59Z</end>
                                                                        </timeInterval>
                                                                        <Point>
                                                                            <position>1</position>
                                                                            <min_Quantity.quantity>1</min_Quantity.quantity>
                                                                            <min_Quantity.quality>A04</min_Quantity.quality>
                                                                            <max_Quantity.quantity>4</max_Quantity.quantity>
                                                                            <max_Quantity.quality>A04</max_Quantity.quality>
                                                                        </Point>
                                                                    </Period>
                                                                </Series>
                                                            </TimeSeries_Series>
                                                        </MarketDocument>
                                                    </RECMMOE_Envelope>
                                                    """
                                    )
                            ),
                    }
            )
    )
    ResponseEntity<Void> minMaxEnvelopeMd(RECMMOEEnvelope minMaxEnvelope);

    @Operation(
            operationId = "GET forwarded min-max envelope market document stream",
            summary = "GET forwarded min-max envelope market document stream",
            description = "Get all new min-max envelope market documents forwarded to the eligible party as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "text/event-stream",
                            schema = @Schema(implementation = RECMMOEEnvelope.class),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                            {
                                              "MessageDocumentHeader": {
                                                "creationDateTime": "2026-02-16T10:11:58Z",
                                                "MetaInformation": {
                                                  "connectionId": "1",
                                                  "requestPermissionId": "aae63ff1-4062-4599-8f4c-686df39138e7",
                                                  "dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a12",
                                                  "documentType": "min-max-envelope",
                                                  "finalCustomerId": "fc-id",
                                                  "dataSourceId": "0743c9d8-3e5f-4575-999b-34f6f83b2075",
                                                  "defaultValues": "minPower=value",
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
                                                "mRID": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a12",
                                                "description": "Test Min-Max Envelope",
                                                "revisionNumber": "1",
                                                "lastModifiedDateTime": "2026-02-16T10:11:58Z",
                                                "comment": "This is a test min-max envelope.",
                                                "sender_MarketParticipant.mRID": {
                                                  "value": "AT003000",
                                                  "codingScheme": "NAT"
                                                },
                                                "receiver_MarketParticipant.mRID": {
                                                  "value": "fc-id",
                                                  "codingScheme": "NAT"
                                                },
                                                "process.processType": "A14",
                                                "TimeSeries_Series": [
                                                  {
                                                    "mRID": "series-1",
                                                    "businessType": "C76",
                                                    "curveType": "A01",
                                                    "resourceTimeSeries.value1ScheduleType": "loadReduction",
                                                    "flowDirection.direction": "A02",
                                                    "registeredResource.mRID": {
                                                      "value": "003114735",
                                                      "codingScheme": "NAT"
                                                    },
                                                    "Series": [
                                                      {
                                                        "Period": [
                                                          {
                                                            "resolution": "P1D",
                                                            "timeInterval": {
                                                              "start": "2026-06-01T00:00:00Z",
                                                              "end": "2026-06-02T23:59:59Z"
                                                            },
                                                            "Point": [
                                                              {
                                                                "position": 1,
                                                                "min_Quantity.quantity": 1,
                                                                "min_Quantity.quality": "1",
                                                                "max_Quantity.quantity": 4,
                                                                "max_Quantity.quality": "3"
                                                              }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    ]
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<RECMMOEEnvelope>> minMaxEnvelopeMdSSE();

    @Operation(
            operationId = "GET min-max envelope market documents forwarded to the eligible party",
            summary = "GET min-max envelope market documents forwarded to the eligible party",
            description = "Query available min-max envelope market documents forwarded to the eligible party",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = RECMMOEEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    [
                                                      {
                                                        "MessageDocumentHeader": {
                                                          "creationDateTime": "2026-02-16T10:11:58Z",
                                                          "MetaInformation": {
                                                            "connectionId": "1",
                                                            "requestPermissionId": "aae63ff1-4062-4599-8f4c-686df39138e7",
                                                            "dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a12",
                                                            "documentType": "min-max-envelope",
                                                            "finalCustomerId": "fc-id",
                                                            "dataSourceId": "0743c9d8-3e5f-4575-999b-34f6f83b2075",
                                                            "defaultValues": "minPower=value",
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
                                                          "mRID": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a12",
                                                          "description": "Test Min-Max Envelope",
                                                          "revisionNumber": "1",
                                                          "lastModifiedDateTime": "2026-02-16T10:11:58Z",
                                                          "comment": "This is a test min-max envelope.",
                                                          "sender_MarketParticipant.mRID": {
                                                            "value": "AT003000",
                                                            "codingScheme": "NAT"
                                                          },
                                                          "receiver_MarketParticipant.mRID": {
                                                            "value": "fc-id",
                                                            "codingScheme": "NAT"
                                                          },
                                                          "process.processType": "A14",
                                                          "TimeSeries_Series": [
                                                            {
                                                              "mRID": "series-1",
                                                              "businessType": "C76",
                                                              "curveType": "A01",
                                                              "resourceTimeSeries.value1ScheduleType": "loadReduction",
                                                              "flowDirection.direction": "A02",
                                                              "registeredResource.mRID": {
                                                                "value": "003114735",
                                                                "codingScheme": "NAT"
                                                              },
                                                              "Series": [
                                                                {
                                                                  "Period": [
                                                                    {
                                                                      "resolution": "P1D",
                                                                      "timeInterval": {
                                                                        "start": "2026-06-01T00:00:00Z",
                                                                        "end": "2026-06-02T23:59:59Z"
                                                                      },
                                                                      "Point": [
                                                                        {
                                                                          "position": 1,
                                                                          "min_Quantity.quantity": 1,
                                                                          "min_Quantity.quality": "1",
                                                                          "max_Quantity.quantity": 4,
                                                                          "max_Quantity.quality": "3"
                                                                        }
                                                                      ]
                                                                    }
                                                                  ]
                                                                }
                                                              ]
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
                                    schema = @Schema(implementation = MinMaxEnvelopeMarketDocuments.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <MinMaxEnvelopeMarketDocuments>
                                                        <RECMMOE_Envelope xmlns="https//eddie.energy/CIM/RECMMOE_v1.12">
                                                            <MessageDocumentHeader>
                                                                <creationDateTime>2026-02-16T10:17:11Z</creationDateTime>
                                                                <MetaInformation>
                                                                    <connectionId>1</connectionId>
                                                                    <requestPermissionId>aae63ff1-4062-4599-8f4c-686df39138e7</requestPermissionId>
                                                                    <dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a12</dataNeedId>
                                                                    <documentType>min-max-envelope</documentType>
                                                                    <finalCustomerId>fc-id</finalCustomerId>
                                                                    <dataSourceId>0743c9d8-3e5f-4575-999b-34f6f83b2075</dataSourceId>
                                                                    <defaultValues>minPower=value</defaultValues>
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
                                                                <mRID>5dc71d7e-e8cd-4403-a3a8-d3c095c97a12</mRID>
                                                                <description>Test Min-Max Envelope</description>
                                                                <revisionNumber>1</revisionNumber>
                                                                <lastModifiedDateTime>2026-02-16T10:17:11Z</lastModifiedDateTime>
                                                                <comment>This is a test min-max envelope.</comment>
                                                                <sender_MarketParticipant.mRID codingScheme="NAT">AT003000</sender_MarketParticipant.mRID>
                                                                <receiver_MarketParticipant.mRID codingScheme="NAT">fc-id</receiver_MarketParticipant.mRID>
                                                                <process.processType>A14</process.processType>
                                                                <period.timeInterval>
                                                                    <start>2026-06-01T00:00Z</start>
                                                                    <end>2026-06-30T23:59Z</end>
                                                                </period.timeInterval>
                                                                <TimeSeries_Series>
                                                                    <mRID>series-1</mRID>
                                                                    <businessType>C76</businessType>
                                                                    <curveType>A01</curveType>
                                                                    <resourceTimeSeries.value1ScheduleType>loadReduction</resourceTimeSeries.value1ScheduleType>
                                                                    <flowDirection.direction>A02</flowDirection.direction>
                                                                    <registeredResource.mRID codingScheme="NAT">003114735</registeredResource.mRID>
                                                                    <Series>
                                                                        <Period>
                                                                            <resolution>PT15M</resolution>
                                                                            <timeInterval>
                                                                                <start>2026-06-01T00:00Z</start>
                                                                                <end>2026-06-30T23:59Z</end>
                                                                            </timeInterval>
                                                                            <Point>
                                                                                <position>1</position>
                                                                                <min_Quantity.quantity>1</min_Quantity.quantity>
                                                                                <min_Quantity.quality>A04</min_Quantity.quality>
                                                                                <max_Quantity.quantity>4</max_Quantity.quantity>
                                                                                <max_Quantity.quality>A04</max_Quantity.quality>
                                                                            </Point>
                                                                        </Period>
                                                                    </Series>
                                                                </TimeSeries_Series>
                                                            </MarketDocument>
                                                        </RECMMOE_Envelope>
                                                    </MinMaxEnvelopeMarketDocuments>
                                                    """
                                    )
                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the min-max envelope market documents forwarded to the eligible party by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the min-max envelope market documents forwarded to the eligible party by connectionId ID",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the min-max envelope market documents forwarded to the eligible party by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the min-max envelope market documents forwarded to the eligible party by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "N[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the min-max envelope market documents forwarded to the eligible party by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the min-max envelope market documents forwarded to the eligible party by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the min-max envelope market documents forwarded to the eligible party by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<MinMaxEnvelopeMarketDocuments> minMaxEnvelopeMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );

    @Operation(
            operationId = "GET energy sharing reference data market document stream",
            summary = "GET energy sharing reference data market document stream",
            description = "Get all new energy sharing reference data market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = ESRDMDEnvelope.class),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                            {
                                              "MarketDocument": {
                                                "type": null,
                                                "mRID": "AT000000000000000000000000000000000",
                                                "description": null,
                                                "revisionNumber": "112",
                                                "createdDateTime": "2025-10-06T07:04:58Z",
                                                "sender_MarketParticipant.mRID": null,
                                                "sender_MarketParticipant.name": "AT000000",
                                                "sender_MarketParticipant.marketRole.type": null,
                                                "receiver_MarketParticipant.mRID": null,
                                                "receiver_MarketParticipant.name": "CC000000",
                                                "receiver_MarketParticipant.marketRole.type": null,
                                                "EnergyCommunity": [
                                                  {
                                                    "DateFrom": "2025-10-05T22:00:00Z",
                                                    "mRID": "ATCC0000DYNAMCC000000000000000000",
                                                    "AccountingPoint": [
                                                      {
                                                        "energySharingParticipationFactor": 100,
                                                        "mRID": {
                                                          "value": "AT0000000000000000000000000000000",
                                                          "codingScheme": "NAT"
                                                        },
                                                        "settlementMethod": null,
                                                        "energySharingEnergyDirection": "A02",
                                                        "meterReadingResolution": null,
                                                        "gridAgreementType": null,
                                                        "administrativeStatus": null,
                                                        "flexibilityContract": null,
                                                        "MeterReadings": null
                                                      }
                                                    ]
                                                  }
                                                ],
                                                "Process": null
                                              },
                                              "MessageDocumentHeader": {
                                                "creationDateTime": "2026-04-30T08:09:13Z",
                                                "MetaInformation": {
                                                  "connectionId": "cid",
                                                  "requestPermissionId": "pid",
                                                  "dataNeedId": "dnid",
                                                  "documentType": "energy-sharing-reference-data-market-document",
                                                  "finalCustomerId": null,
                                                  "dataSourceId": null,
                                                  "defaultValues": null,
                                                  "regionConnector": "at-eda",
                                                  "regionCountry": "AT",
                                                  "Asset": null
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<ESRDMDEnvelope>> energySharingReferenceDataMdSSE();

    @Operation(
            operationId = "GET energy sharing reference data market document stream",
            summary = "GET energy sharing reference data market document stream",
            description = "Get all new energy sharing reference data market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ESRDMDEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    [{
                                                      "MarketDocument": {
                                                        "type": null,
                                                        "mRID": "AT000000000000000000000000000000000",
                                                        "description": null,
                                                        "revisionNumber": "112",
                                                        "createdDateTime": "2025-10-06T07:04:58Z",
                                                        "sender_MarketParticipant.mRID": null,
                                                        "sender_MarketParticipant.name": "AT000000",
                                                        "sender_MarketParticipant.marketRole.type": null,
                                                        "receiver_MarketParticipant.mRID": null,
                                                        "receiver_MarketParticipant.name": "CC000000",
                                                        "receiver_MarketParticipant.marketRole.type": null,
                                                        "EnergyCommunity": [
                                                          {
                                                            "DateFrom": "2025-10-05T22:00:00Z",
                                                            "mRID": "ATCC0000DYNAMCC000000000000000000",
                                                            "AccountingPoint": [
                                                              {
                                                                "energySharingParticipationFactor": 100,
                                                                "mRID": {
                                                                  "value": "AT0000000000000000000000000000000",
                                                                  "codingScheme": "NAT"
                                                                },
                                                                "settlementMethod": null,
                                                                "energySharingEnergyDirection": "A02",
                                                                "meterReadingResolution": null,
                                                                "gridAgreementType": null,
                                                                "administrativeStatus": null,
                                                                "flexibilityContract": null,
                                                                "MeterReadings": null
                                                              }
                                                            ]
                                                          }
                                                        ],
                                                        "Process": null
                                                      },
                                                      "MessageDocumentHeader": {
                                                        "creationDateTime": "2026-04-30T08:09:13Z",
                                                        "MetaInformation": {
                                                          "connectionId": "cid",
                                                          "requestPermissionId": "pid",
                                                          "dataNeedId": "dnid",
                                                          "documentType": "energy-sharing-reference-data-market-document",
                                                          "finalCustomerId": null,
                                                          "dataSourceId": null,
                                                          "defaultValues": null,
                                                          "regionConnector": "at-eda",
                                                          "regionCountry": "AT",
                                                          "Asset": null
                                                        }
                                                      }
                                                    }]
                                                    """
                                    )
                            ),
                            @Content(mediaType = "application/xml",
                                    array = @ArraySchema(schema = @Schema(implementation = ESRDMDEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=xml
                                            value = """
                                                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                                                    <ns:ESRDMD_Envelope xmlns:ns="https://eddie.energy/CIM/CEEDS_EnergySharingReferenceDataMarketDocument_v1.12_annotated.xsd">
                                                        <ns:MarketDocument>
                                                            <ns:mRID>AT000000000000000000000000000000000</ns:mRID>
                                                            <ns:revisionNumber>112</ns:revisionNumber>
                                                            <ns:createdDateTime>2025-10-06T07:04:58Z</ns:createdDateTime>
                                                            <ns:sender_MarketParticipant.name>AT000000</ns:sender_MarketParticipant.name>
                                                            <ns:receiver_MarketParticipant.name>CC000000</ns:receiver_MarketParticipant.name>
                                                            <ns:EnergyCommunity>
                                                                <ns:DateFrom>2025-10-05T22:00:00Z</ns:DateFrom>
                                                                <ns:mRID>ATCC0000DYNAMCC000000000000000000</ns:mRID>
                                                                <ns:AccountingPoint>
                                                                    <ns:energySharingParticipationFactor>100</ns:energySharingParticipationFactor>
                                                                    <ns:mRID codingScheme="NAT">AT0000000000000000000000000000000</ns:mRID>
                                                                    <ns:energySharingEnergyDirection>A02</ns:energySharingEnergyDirection>
                                                                </ns:AccountingPoint>
                                                            </ns:EnergyCommunity>
                                                        </ns:MarketDocument>
                                                        <ns:MessageDocumentHeader>
                                                            <ns:creationDateTime>2026-04-30T07:42:42Z</ns:creationDateTime>
                                                            <ns:MetaInformation>
                                                                <ns:connectionId>cid</ns:connectionId>
                                                                <ns:requestPermissionId>pid</ns:requestPermissionId>
                                                                <ns:dataNeedId>dnid</ns:dataNeedId>
                                                                <ns:documentType>energy-sharing-reference-data-market-document</ns:documentType>
                                                                <ns:regionConnector>at-eda</ns:regionConnector>
                                                                <ns:regionCountry>AT</ns:regionCountry>
                                                            </ns:MetaInformation>
                                                        </ns:MessageDocumentHeader>
                                                    </ns:ESRDMD_Envelope>
                                                    """
                                    )
                            )
                    }
            ),

            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the energy sharing reference data market documents by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the energy sharing reference data market documents by connectionId ID",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the energy sharing reference data market documents by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the energy sharing reference data market documents by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "N[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the energy sharing reference data market documents by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the energy sharing reference data market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the energy sharing reference data market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<EnergySharingReferenceDataMarketDocuments> energySharingReferenceDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );


    @Operation(
            operationId = "GET request permission market document stream",
            summary = "GET request permission market document stream",
            description = "Get all new request permission market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = RequestPermissionEnvelope.class),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                            {
                                              "MarketDocument": {
                                                "mRID": "301287d3-2d73-4158-8380-089e09278ed6",
                                                "description": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                "revisionNumber": "112",
                                                "type": "B48",
                                                "sender_MarketParticipant.mRID": {
                                                  "value": "sim",
                                                  "codingScheme": "NDE"
                                                },
                                                "sender_MarketParticipant.marketRole.type": "A20",
                                                "receiver_MarketParticipant.mRID": {
                                                  "value": "sim",
                                                  "codingScheme": "NDE"
                                                },
                                                "receiver_MarketParticipant.marketRole.type": "A59",
                                                "process.processType": "A74",
                                                "period.timeInterval": {
                                                  "start": "2021-01-01T00:00Z",
                                                  "end": "9999-12-31T00:00Z"
                                                },
                                                "Request_Permission": {
                                                  "mRID": "301287d3-2d73-4158-8380-089e09278ed6",
                                                  "createdDateTime": "2026-05-19T11:15:43Z",
                                                  "transmissionSchedule": "P1D",
                                                  "AccountingPoint": [
                                                    {
                                                      "mRID": {
                                                        "value": "1",
                                                        "codingScheme": "NDE"
                                                      },
                                                      "name": null
                                                    }
                                                  ],
                                                  "MktActivityRecord": [
                                                    {
                                                      "mRID": "bc2db47b-e6fe-418c-bf22-22c226964ef5",
                                                      "createdDateTime": "2026-05-19T11:15:43Z",
                                                      "description": "REQUIRES_EXTERNAL_TERMINATION",
                                                      "type": "sim",
                                                      "name": null,
                                                      "reason": null,
                                                      "status": "A08"
                                                    }
                                                  ],
                                                  "Reason": null,
                                                  "TimeSeries": null
                                                }
                                              },
                                              "MessageDocumentHeader": {
                                                "creationDateTime": "2026-05-19T11:15:43Z",
                                                "MetaInformation": {
                                                  "connectionId": "1",
                                                  "requestPermissionId": "301287d3-2d73-4158-8380-089e09278ed6",
                                                  "dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                  "documentType": "request-permission-market-document",
                                                  "finalCustomerId": null,
                                                  "dataSourceId": null,
                                                  "defaultValues": null,
                                                  "regionConnector": "sim",
                                                  "regionCountry": "DE",
                                                  "Asset": null
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<RequestPermissionEnvelope>> requestPermissionMdSSE();


    @Operation(
            operationId = "GET request permission market documents",
            summary = "GET request permission market documents",
            description = "Get all past request permission market documents",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = RequestPermissionEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    [{
                                                      "MarketDocument": {
                                                        "mRID": "301287d3-2d73-4158-8380-089e09278ed6",
                                                        "description": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                        "revisionNumber": "112",
                                                        "type": "B48",
                                                        "sender_MarketParticipant.mRID": {
                                                          "value": "sim",
                                                          "codingScheme": "NDE"
                                                        },
                                                        "sender_MarketParticipant.marketRole.type": "A20",
                                                        "receiver_MarketParticipant.mRID": {
                                                          "value": "sim",
                                                          "codingScheme": "NDE"
                                                        },
                                                        "receiver_MarketParticipant.marketRole.type": "A59",
                                                        "process.processType": "A74",
                                                        "period.timeInterval": {
                                                          "start": "2021-01-01T00:00Z",
                                                          "end": "9999-12-31T00:00Z"
                                                        },
                                                        "Request_Permission": {
                                                          "mRID": "301287d3-2d73-4158-8380-089e09278ed6",
                                                          "createdDateTime": "2026-05-19T11:15:43Z",
                                                          "transmissionSchedule": "P1D",
                                                          "AccountingPoint": [
                                                            {
                                                              "mRID": {
                                                                "value": "1",
                                                                "codingScheme": "NDE"
                                                              },
                                                              "name": null
                                                            }
                                                          ],
                                                          "MktActivityRecord": [
                                                            {
                                                              "mRID": "bc2db47b-e6fe-418c-bf22-22c226964ef5",
                                                              "createdDateTime": "2026-05-19T11:15:43Z",
                                                              "description": "REQUIRES_EXTERNAL_TERMINATION",
                                                              "type": "sim",
                                                              "name": null,
                                                              "reason": null,
                                                              "status": "A08"
                                                            }
                                                          ],
                                                          "Reason": null,
                                                          "TimeSeries": null
                                                        }
                                                      },
                                                      "MessageDocumentHeader": {
                                                        "creationDateTime": "2026-05-19T11:15:43Z",
                                                        "MetaInformation": {
                                                          "connectionId": "1",
                                                          "requestPermissionId": "301287d3-2d73-4158-8380-089e09278ed6",
                                                          "dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                          "documentType": "request-permission-market-document",
                                                          "finalCustomerId": null,
                                                          "dataSourceId": null,
                                                          "defaultValues": null,
                                                          "regionConnector": "sim",
                                                          "regionCountry": "DE",
                                                          "Asset": null
                                                        }
                                                      }
                                                    }]
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = RequestPermissionMarketDocuments.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                                                    <RequestPermissionMarketDocuments xmlns:ns="https://eddie.energy/CEEDS_RequestPermissionDocument_annotated_v1.12.xsd" >
                                                        <ns:RequestPermission_Envelope>
                                                            <ns:MarketDocument>
                                                                <ns:mRID>301287d3-2d73-4158-8380-089e09278ed6</ns:mRID>
                                                                <ns:description>9bd0668f-cc19-40a8-99db-dc2cb2802b17</ns:description>
                                                                <ns:revisionNumber>112</ns:revisionNumber>
                                                                <ns:type>B48</ns:type>
                                                                <ns:sender_MarketParticipant.mRID codingScheme="NDE">sim</ns:sender_MarketParticipant.mRID>
                                                                <ns:sender_MarketParticipant.marketRole.type>A20</ns:sender_MarketParticipant.marketRole.type>
                                                                <ns:receiver_MarketParticipant.mRID codingScheme="NDE">sim</ns:receiver_MarketParticipant.mRID>
                                                                <ns:receiver_MarketParticipant.marketRole.type>A59</ns:receiver_MarketParticipant.marketRole.type>
                                                                <ns:process.processType>A74</ns:process.processType>
                                                                <ns:period.timeInterval>
                                                                    <ns:start>2021-01-01T00:00Z</ns:start>
                                                                    <ns:end>9999-12-31T00:00Z</ns:end>
                                                                </ns:period.timeInterval>
                                                                <ns:Request_Permission>
                                                                    <ns:mRID>301287d3-2d73-4158-8380-089e09278ed6</ns:mRID>
                                                                    <ns:createdDateTime>2026-05-19T11:48:56Z</ns:createdDateTime>
                                                                    <ns:AccountingPoint>
                                                                        <ns:mRID codingScheme="NDE">1</ns:mRID>
                                                                    </ns:AccountingPoint>
                                                                    <ns:MktActivityRecord>
                                                                        <ns:mRID>456d8c55-971c-4694-b390-8c7bd5818660</ns:mRID>
                                                                        <ns:createdDateTime>2026-05-19T11:48:56Z</ns:createdDateTime>
                                                                        <ns:description>EXTERNALLY_TERMINATED</ns:description>
                                                                        <ns:type>sim</ns:type>
                                                                        <ns:status>A16</ns:status>
                                                                    </ns:MktActivityRecord>
                                                                </ns:Request_Permission>
                                                            </ns:MarketDocument>
                                                            <ns:MessageDocumentHeader>
                                                                <ns:creationDateTime>2026-05-19T11:48:56Z</ns:creationDateTime>
                                                                <ns:MetaInformation>
                                                                    <ns:connectionId>1</ns:connectionId>
                                                                    <ns:requestPermissionId>301287d3-2d73-4158-8380-089e09278ed6</ns:requestPermissionId>
                                                                    <ns:dataNeedId>9bd0668f-cc19-40a8-99db-dc2cb2802b17</ns:dataNeedId>
                                                                    <ns:documentType>request-permission-market-document</ns:documentType>
                                                                    <ns:regionConnector>sim</ns:regionConnector>
                                                                    <ns:regionCountry>DE</ns:regionCountry>
                                                                </ns:MetaInformation>
                                                            </ns:MessageDocumentHeader>
                                                        </ns:RequestPermission_Envelope>
                                                    </RequestPermissionMarketDocuments>
                                                    """
                                    )
                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the request permission market documents by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the request permission market documents by connectionId ID",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the request permission market documents by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the request permission market documents by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "N[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the request permission market documents by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the request permission market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the request permission market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<RequestPermissionMarketDocuments> requestPermissionMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );
}
