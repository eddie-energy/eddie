// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.cim.v1_12;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.dto.v1_12.NearRealTimeDataMarketDocuments;
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
                                    schema = @Schema(implementation = PermissionEnvelope.class),
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
                                                        "sender_MarketParticipant.marketRole.type": "CONNECTING_SYSTEM_OPERATOR",
                                                        "receiver_MarketParticipant.mRID": {
                                                          "value": "88e0fc2c-4ea7-4850-a736-8b9742757518",
                                                          "codingScheme": "NAT"
                                                        },
                                                        "receiver_MarketParticipant.name": "Max Mustermann",
                                                        "receiver_MarketParticipant.marketRole.type": "FINAL_CUSTOMER",
                                                        "process.processType": "MIN_MAX_ENVELOPE",
                                                        "period.timeInterval": {
                                                          "start": "2026-06-01T00:00:00Z",
                                                          "end": "2026-06-02T23:59:59Z"
                                                        },
                                                        "TimeSeries_Series": [
                                                          {
                                                            "mRID": "series-1",
                                                            "businessType": "MIN_MAX_ENVELOPE",
                                                            "curveType": "MIN_MAX_ENVELOPE",
                                                            "resourceTimeSeries.value1ScheduleType": "loadReduction",
                                                            "flowDirection.direction": "CONSUMPTION",
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
                                    schema = @Schema(implementation = PermissionEnvelope.class),
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
                                                            <mRID>5dc71d7e-e8cd-4403-a3a8-d3c095c97a12</mRID>
                                                            <description>Test Min-Max Envelope</description>
                                                            <revisionNumber>1</revisionNumber>
                                                            <lastModifiedDateTime>2026-02-16T10:17:11Z</lastModifiedDateTime>
                                                            <comment>This is a test min-max envelope.</comment>
                                                            <sender_MarketParticipant.mRID codingScheme="NAT">AT003000</sender_MarketParticipant.mRID>
                                                            <sender_MarketParticipant.name>Netz Oberösterreich GmbH</sender_MarketParticipant.name>
                                                            <sender_MarketParticipant.marketRole.type>CONNECTING_SYSTEM_OPERATOR</sender_MarketParticipant.marketRole.type>
                                                            <receiver_MarketParticipant.mRID codingScheme="NAT">88e0fc2c-4ea7-4850-a736-8b9742757518
                                                            </receiver_MarketParticipant.mRID>
                                                            <receiver_MarketParticipant.name>Max Mustermann</receiver_MarketParticipant.name>
                                                            <receiver_MarketParticipant.marketRole.type>FINAL_CUSTOMER</receiver_MarketParticipant.marketRole.type>
                                                            <process.processType>MIN_MAX_ENVELOPE</process.processType>
                                                            <period.timeInterval>
                                                                <start>2026-06-01T00:00:00Z</start>
                                                                <end>2026-06-30T23:59:59Z</end>
                                                            </period.timeInterval>
                                                            <TimeSeries_Series>
                                                                <TimeSeries_Series>
                                                                    <mRID>series-1</mRID>
                                                                    <businessType>MIN_MAX_ENVELOPE</businessType>
                                                                    <curveType>MIN_MAX_ENVELOPE</curveType>
                                                                    <resourceTimeSeries.value1ScheduleType>loadReduction</resourceTimeSeries.value1ScheduleType>
                                                                    <flowDirection.direction>CONSUMPTION</flowDirection.direction>
                                                                    <registeredResource.mRID codingScheme="NAT">003114735</registeredResource.mRID>
                                                                    <registeredResource.name>Test Connection Point</registeredResource.name>
                                                                    <registeredResource.description>This is a test connection point for the min-max envelope.
                                                                    </registeredResource.description>
                                                                    <Series>
                                                                        <Series>
                                                                            <Period>
                                                                                <Period>
                                                                                    <resolution xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                                                                                    <timeInterval>
                                                                                        <start>2026-06-01T00:00:00Z</start>
                                                                                        <end>2026-06-30T23:59:59Z</end>
                                                                                    </timeInterval>
                                                                                    <Point>
                                                                                        <Point>
                                                                                            <position>1</position>
                                                                                            <min_Quantity.quantity>1</min_Quantity.quantity>
                                                                                            <min_Quantity.quality>1</min_Quantity.quality>
                                                                                            <max_Quantity.quantity>4</max_Quantity.quantity>
                                                                                            <max_Quantity.quality>3</max_Quantity.quality>
                                                                                        </Point>
                                                                                    </Point>
                                                                                </Period>
                                                                            </Period>
                                                                        </Series>
                                                                    </Series>
                                                                </TimeSeries_Series>
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
}
