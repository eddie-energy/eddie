package energy.eddie.outbound.rest.web.cim.v0_82;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.dto.AccountingPointDataMarketDocuments;
import energy.eddie.outbound.rest.dto.PermissionMarketDocuments;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocuments;
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

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
@Tag(name = "CIM v0.82 Documents", description = "Provides endpoints for CIM v0.82 documents, such as validated historical data market documents.")
public interface CimSwagger {

    @Operation(
            operationId = "GET validated historical data market document stream",
            summary = "GET validated historical data market document stream",
            description = "Get all new validated historical data market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = ValidatedHistoricalDataEnvelope.class),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                              {
                                                  "MessageDocumentHeader": {
                                                    "creationDateTime": "2025-07-23T10:31:30Z",
                                                    "MessageDocumentHeader_MetaInformation": {
                                                      "connectionid": "1",
                                                      "permissionid": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                      "dataNeedid": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                      "dataType": "validated-historical-data-market-document",
                                                      "MessageDocumentHeader_Region": {
                                                        "connector": "sim",
                                                        "country": "NAT"
                                                      }
                                                    }
                                                  },
                                                  "ValidatedHistoricalData_MarketDocument": {
                                                    "mRID": "3847de2f-45f8-44e8-8a68-591635477f94",
                                                    "revisionNumber": "0.82",
                                                    "type": "A45",
                                                    "createdDateTime": "2025-07-23T10:31:30Z",
                                                    "description": null,
                                                    "sender_MarketParticipant.mRID": {
                                                      "codingScheme": "A01",
                                                      "value": "sim"
                                                    },
                                                    "sender_MarketParticipant.marketRole.type": "A26",
                                                    "receiver_MarketParticipant.mRID": {
                                                      "codingScheme": "NAT",
                                                      "value": "sim"
                                                    },
                                                    "receiver_MarketParticipant.marketRole.type": "A13",
                                                    "process.processType": "A16",
                                                    "period.timeInterval": {
                                                      "start": "2024-12-30T09:49Z",
                                                      "end": "2024-12-30T10:04Z"
                                                    },
                                                    "TimeSeriesList": {
                                                      "TimeSeries": [
                                                        {
                                                          "mRID": "edf4c10d-91f6-4024-ad5f-43b50a433fc6",
                                                          "businessType": "A04",
                                                          "product": "8716867000030",
                                                          "version": "1.0",
                                                          "in_Domain.mRID": null,
                                                          "out_Domain.mRID": null,
                                                          "flowDirection.direction": "A02",
                                                          "marketEvaluationPoint.mRID": {
                                                            "codingScheme": "NFR",
                                                            "value": "mid"
                                                          },
                                                          "marketEvaluationPoint.meterReadings.mRID": null,
                                                          "marketEvaluationPoint.meterReadings.readings.mRID": null,
                                                          "marketEvaluationPoint.meterReadings.readings.ReadingType.accumulate": null,
                                                          "marketEvaluationPoint.meterReadings.readings.ReadingType.aggregation": "26",
                                                          "marketEvaluationPoint.meterReadings.readings.ReadingType.commodity": "0",
                                                          "marketEvaluationPoint.usagePointLocation.geoInfoReference": null,
                                                          "energy_Measurement_Unit.name": "WTT",
                                                          "energyQuality_Measurement_Unit.name": null,
                                                          "registration_DateAndOrTime.dateTime": null,
                                                          "RegisteredResource": null,
                                                          "Series_PeriodList": {
                                                            "Series_Period": [
                                                              {
                                                                "timeInterval": {
                                                                  "start": "2024-12-30T09:49Z",
                                                                  "end": "2024-12-30T10:04Z"
                                                                },
                                                                "resolution": "PT15M",
                                                                "PointList": {
                                                                  "Point": [
                                                                    {
                                                                      "position": "0",
                                                                      "energyQuality_Quantity.quantity": null,
                                                                      "energyQuality_Quantity.quality": null,
                                                                      "energy_Quantity.quantity": 10.0,
                                                                      "energy_Quantity.quality": "A04"
                                                                    }
                                                                  ]
                                                                },
                                                                "ReasonList": null
                                                              }
                                                            ]
                                                          },
                                                          "ReasonList": {
                                                            "Reason": [
                                                              {
                                                                "text": null,
                                                                "code": "999"
                                                              }
                                                            ]
                                                          }
                                                        }
                                                      ]
                                                    }
                                                  }
                                                }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<ValidatedHistoricalDataEnvelope>> validatedHistoricalDataMdSSE();


    @Operation(
            operationId = "GET validated historical data market documents",
            summary = "GET validated historical data market documents",
            description = "Get all past validated historical data market documents",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ValidatedHistoricalDataEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                      [{
                                                          "MessageDocumentHeader": {
                                                            "creationDateTime": "2025-07-23T10:31:30Z",
                                                            "MessageDocumentHeader_MetaInformation": {
                                                              "connectionid": "1",
                                                              "permissionid": "ffcb8491-1f82-4d9d-9ddf-f1312796045a",
                                                              "dataNeedid": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                              "dataType": "validated-historical-data-market-document",
                                                              "MessageDocumentHeader_Region": {
                                                                "connector": "sim",
                                                                "country": "NAT"
                                                              }
                                                            }
                                                          },
                                                          "ValidatedHistoricalData_MarketDocument": {
                                                            "mRID": "3847de2f-45f8-44e8-8a68-591635477f94",
                                                            "revisionNumber": "0.82",
                                                            "type": "A45",
                                                            "createdDateTime": "2025-07-23T10:31:30Z",
                                                            "description": null,
                                                            "sender_MarketParticipant.mRID": {
                                                              "codingScheme": "A01",
                                                              "value": "sim"
                                                            },
                                                            "sender_MarketParticipant.marketRole.type": "A26",
                                                            "receiver_MarketParticipant.mRID": {
                                                              "codingScheme": "NAT",
                                                              "value": "sim"
                                                            },
                                                            "receiver_MarketParticipant.marketRole.type": "A13",
                                                            "process.processType": "A16",
                                                            "period.timeInterval": {
                                                              "start": "2024-12-30T09:49Z",
                                                              "end": "2024-12-30T10:04Z"
                                                            },
                                                            "TimeSeriesList": {
                                                              "TimeSeries": [
                                                                {
                                                                  "mRID": "edf4c10d-91f6-4024-ad5f-43b50a433fc6",
                                                                  "businessType": "A04",
                                                                  "product": "8716867000030",
                                                                  "version": "1.0",
                                                                  "in_Domain.mRID": null,
                                                                  "out_Domain.mRID": null,
                                                                  "flowDirection.direction": "A02",
                                                                  "marketEvaluationPoint.mRID": {
                                                                    "codingScheme": "NFR",
                                                                    "value": "mid"
                                                                  },
                                                                  "marketEvaluationPoint.meterReadings.mRID": null,
                                                                  "marketEvaluationPoint.meterReadings.readings.mRID": null,
                                                                  "marketEvaluationPoint.meterReadings.readings.ReadingType.accumulate": null,
                                                                  "marketEvaluationPoint.meterReadings.readings.ReadingType.aggregation": "26",
                                                                  "marketEvaluationPoint.meterReadings.readings.ReadingType.commodity": "0",
                                                                  "marketEvaluationPoint.usagePointLocation.geoInfoReference": null,
                                                                  "energy_Measurement_Unit.name": "WTT",
                                                                  "energyQuality_Measurement_Unit.name": null,
                                                                  "registration_DateAndOrTime.dateTime": null,
                                                                  "RegisteredResource": null,
                                                                  "Series_PeriodList": {
                                                                    "Series_Period": [
                                                                      {
                                                                        "timeInterval": {
                                                                          "start": "2024-12-30T09:49Z",
                                                                          "end": "2024-12-30T10:04Z"
                                                                        },
                                                                        "resolution": "PT15M",
                                                                        "PointList": {
                                                                          "Point": [
                                                                            {
                                                                              "position": "0",
                                                                              "energyQuality_Quantity.quantity": null,
                                                                              "energyQuality_Quantity.quality": null,
                                                                              "energy_Quantity.quantity": 10.0,
                                                                              "energy_Quantity.quality": "A04"
                                                                            }
                                                                          ]
                                                                        },
                                                                        "ReasonList": null
                                                                      }
                                                                    ]
                                                                  },
                                                                  "ReasonList": {
                                                                    "Reason": [
                                                                      {
                                                                        "text": null,
                                                                        "code": "999"
                                                                      }
                                                                    ]
                                                                  }
                                                                }
                                                              ]
                                                            }
                                                          }
                                                        }]
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = ValidatedHistoricalDataMarketDocuments.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                                                    <ValidatedHistoricalDataMarketDocuments xmlns:ns2="http://www.eddie.energy/VHD/EDD01/20240614">
                                                        <ns2:ValidatedHistoricalData_Envelope>
                                                            <ns2:MessageDocumentHeader>
                                                                <ns2:creationDateTime>2025-07-23T10:31:30Z</ns2:creationDateTime>
                                                                <ns2:MessageDocumentHeader_MetaInformation>
                                                                    <ns2:connectionid>1</ns2:connectionid>
                                                                    <ns2:permissionid>ffcb8491-1f82-4d9d-9ddf-f1312796045a</ns2:permissionid>
                                                                    <ns2:dataNeedid>9bd0668f-cc19-40a8-99db-dc2cb2802b17</ns2:dataNeedid>
                                                                    <ns2:dataType>validated-historical-data-market-document</ns2:dataType>
                                                                    <ns2:MessageDocumentHeader_Region>
                                                                        <ns2:connector>sim</ns2:connector>
                                                                        <ns2:country>NAT</ns2:country>
                                                                    </ns2:MessageDocumentHeader_Region>
                                                                </ns2:MessageDocumentHeader_MetaInformation>
                                                            </ns2:MessageDocumentHeader>
                                                            <ns2:ValidatedHistoricalData_MarketDocument>
                                                                <ns2:mRID>3847de2f-45f8-44e8-8a68-591635477f94</ns2:mRID>
                                                                <ns2:revisionNumber>0.82</ns2:revisionNumber>
                                                                <ns2:type>A45</ns2:type>
                                                                <ns2:createdDateTime>2025-07-23T10:31:30Z</ns2:createdDateTime>
                                                                <ns2:sender_MarketParticipant.mRID>
                                                                    <ns2:codingScheme>A01</ns2:codingScheme>
                                                                    <ns2:value>sim</ns2:value>
                                                                </ns2:sender_MarketParticipant.mRID>
                                                                <ns2:sender_MarketParticipant.marketRole.type>A26</ns2:sender_MarketParticipant.marketRole.type>
                                                                <ns2:receiver_MarketParticipant.mRID>
                                                                    <ns2:codingScheme>NAT</ns2:codingScheme>
                                                                    <ns2:value>sim</ns2:value>
                                                                </ns2:receiver_MarketParticipant.mRID>
                                                                <ns2:receiver_MarketParticipant.marketRole.type>A13</ns2:receiver_MarketParticipant.marketRole.type>
                                                                <ns2:process.processType>A16</ns2:process.processType>
                                                                <ns2:period.timeInterval>
                                                                    <ns2:start>2024-12-30T09:49Z</ns2:start>
                                                                    <ns2:end>2024-12-30T10:04Z</ns2:end>
                                                                </ns2:period.timeInterval>
                                                                <ns2:TimeSeriesList>
                                                                    <ns2:TimeSeries>
                                                                        <ns2:mRID>edf4c10d-91f6-4024-ad5f-43b50a433fc6</ns2:mRID>
                                                                        <ns2:businessType>A04</ns2:businessType>
                                                                        <ns2:product>8716867000030</ns2:product>
                                                                        <ns2:version>1.0</ns2:version>
                                                                        <ns2:flowDirection.direction>A02</ns2:flowDirection.direction>
                                                                        <ns2:marketEvaluationPoint.mRID>
                                                                            <ns2:codingScheme>NFR</ns2:codingScheme>
                                                                            <ns2:value>mid</ns2:value>
                                                                        </ns2:marketEvaluationPoint.mRID>
                                                                        <ns2:marketEvaluationPoint.meterReadings.readings.ReadingType.aggregation>26
                                                                        </ns2:marketEvaluationPoint.meterReadings.readings.ReadingType.aggregation>
                                                                        <ns2:marketEvaluationPoint.meterReadings.readings.ReadingType.commodity>0
                                                                        </ns2:marketEvaluationPoint.meterReadings.readings.ReadingType.commodity>
                                                                        <ns2:energy_Measurement_Unit.name>WTT</ns2:energy_Measurement_Unit.name>
                                                                        <ns2:Series_PeriodList>
                                                                            <ns2:Series_Period>
                                                                                <ns2:timeInterval>
                                                                                    <ns2:start>2024-12-30T09:49Z</ns2:start>
                                                                                    <ns2:end>2024-12-30T10:04Z</ns2:end>
                                                                                </ns2:timeInterval>
                                                                                <ns2:resolution>PT15M</ns2:resolution>
                                                                                <ns2:PointList>
                                                                                    <ns2:Point>
                                                                                        <ns2:position>0</ns2:position>
                                                                                        <ns2:energy_Quantity.quantity>10.0</ns2:energy_Quantity.quantity>
                                                                                        <ns2:energy_Quantity.quality>A04</ns2:energy_Quantity.quality>
                                                                                    </ns2:Point>
                                                                                </ns2:PointList>
                                                                            </ns2:Series_Period>
                                                                        </ns2:Series_PeriodList>
                                                                        <ns2:ReasonList>
                                                                            <ns2:Reason>
                                                                                <ns2:code>999</ns2:code>
                                                                            </ns2:Reason>
                                                                        </ns2:ReasonList>
                                                                    </ns2:TimeSeries>
                                                                </ns2:TimeSeriesList>
                                                            </ns2:ValidatedHistoricalData_MarketDocument>
                                                        </ns2:ValidatedHistoricalData_Envelope>
                                                    </ValidatedHistoricalDataMarketDocuments>
                                                    """
                                    )

                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the validated historical data market documents by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the validated historical data market documents by connectionId ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the validated historical data market documents by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the validated historical data market documents by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "N[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the validated historical data market documents by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the validated historical data market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the validated historical data market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<ValidatedHistoricalDataMarketDocuments> validatedHistoricalDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );


    @Operation(
            operationId = "GET permission market document stream",
            summary = "GET permission market document stream",
            description = "Get all new permission market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = PermissionEnvelope.class),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                            {
                                              "mRID": "341ddb3c-1847-4b2d-be19-e6c379cfbd53",
                                              "revisionNumber": "0.82",
                                              "type": "Z04",
                                              "createdDateTime": "2025-07-29T06:26:48Z",
                                              "description": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                              "sender_MarketParticipant.mRID": {
                                                "codingScheme": "NAT",
                                                "value": "sim"
                                              },
                                              "sender_MarketParticipant.marketRole.type": "A20",
                                              "receiver_MarketParticipant.mRID": {
                                                "codingScheme": "NAT",
                                                "value": "sim"
                                              },
                                              "receiver_MarketParticipant.marketRole.type": "A50",
                                              "process.processType": "A55",
                                              "period.timeInterval": {
                                                "start": "2021-01-01T00:00Z",
                                                "end": "9999-12-31T00:00Z"
                                              },
                                              "PermissionList": {
                                                "Permission": [
                                                  {
                                                    "permission.mRID": "341ddb3c-1847-4b2d-be19-e6c379cfbd53",
                                                    "createdDateTime": "2025-07-29T06:26:48Z",
                                                    "transmissionSchedule": null,
                                                    "marketEvaluationPoint.mRID": {
                                                      "codingScheme": "NAT",
                                                      "value": "id"
                                                    },
                                                    "TimeSeriesList": {
                                                      "TimeSeries": null
                                                    },
                                                    "MktActivityRecordList": {
                                                      "MktActivityRecord": [
                                                        {
                                                          "mRID": "963e2eec-e875-4cfc-913d-1ea8e2d745d2",
                                                          "createdDateTime": "2025-07-29T06:26:48Z",
                                                          "description": "FULFILLED",
                                                          "type": "sim",
                                                          "reason": null,
                                                          "name": null,
                                                          "status": "Confirmed"
                                                        }
                                                      ]
                                                    },
                                                    "ReasonList": {
                                                      "Reason": null
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
    ResponseEntity<Flux<PermissionEnvelope>> permissionMdSSE();


    @Operation(
            operationId = "GET permission market documents",
            summary = "GET permission market documents",
            description = "Get all past permission market documents",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = PermissionEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    [{
                                                      "mRID": "341ddb3c-1847-4b2d-be19-e6c379cfbd53",
                                                      "revisionNumber": "0.82",
                                                      "type": "Z04",
                                                      "createdDateTime": "2025-07-29T06:26:48Z",
                                                      "description": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                      "sender_MarketParticipant.mRID": {
                                                        "codingScheme": "NAT",
                                                        "value": "sim"
                                                      },
                                                      "sender_MarketParticipant.marketRole.type": "A20",
                                                      "receiver_MarketParticipant.mRID": {
                                                        "codingScheme": "NAT",
                                                        "value": "sim"
                                                      },
                                                      "receiver_MarketParticipant.marketRole.type": "A50",
                                                      "process.processType": "A55",
                                                      "period.timeInterval": {
                                                        "start": "2021-01-01T00:00Z",
                                                        "end": "9999-12-31T00:00Z"
                                                      },
                                                      "PermissionList": {
                                                        "Permission": [
                                                          {
                                                            "permission.mRID": "341ddb3c-1847-4b2d-be19-e6c379cfbd53",
                                                            "createdDateTime": "2025-07-29T06:26:48Z",
                                                            "transmissionSchedule": null,
                                                            "marketEvaluationPoint.mRID": {
                                                              "codingScheme": "NAT",
                                                              "value": "id"
                                                            },
                                                            "TimeSeriesList": {
                                                              "TimeSeries": null
                                                            },
                                                            "MktActivityRecordList": {
                                                              "MktActivityRecord": [
                                                                {
                                                                  "mRID": "963e2eec-e875-4cfc-913d-1ea8e2d745d2",
                                                                  "createdDateTime": "2025-07-29T06:26:48Z",
                                                                  "description": "FULFILLED",
                                                                  "type": "sim",
                                                                  "reason": null,
                                                                  "name": null,
                                                                  "status": "Confirmed"
                                                                }
                                                              ]
                                                            },
                                                            "ReasonList": {
                                                              "Reason": null
                                                            }
                                                          }
                                                        ]
                                                      }
                                                    }]
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = PermissionMarketDocuments.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <PermissionMarketDocuments>
                                                        <ns2:Permission_Envelope>
                                                            <ns2:MessageDocumentHeader>
                                                                <ns2:creationDateTime>2025-07-29T06:26:48Z</ns2:creationDateTime>
                                                                <ns2:MessageDocumentHeader_MetaInformation>
                                                                    <ns2:connectionid>id</ns2:connectionid>
                                                                    <ns2:permissionid>341ddb3c-1847-4b2d-be19-e6c379cfbd53</ns2:permissionid>
                                                                    <ns2:dataNeedid>9bd0668f-cc19-40a8-99db-dc2cb2802b17</ns2:dataNeedid>
                                                                    <ns2:dataType>permission-market-document</ns2:dataType>
                                                                    <ns2:MessageDocumentHeader_Region>
                                                                        <ns2:connector>sim</ns2:connector>
                                                                        <ns2:country>NDE</ns2:country>
                                                                    </ns2:MessageDocumentHeader_Region>
                                                                </ns2:MessageDocumentHeader_MetaInformation>
                                                            </ns2:MessageDocumentHeader>
                                                            <ns2:Permission_MarketDocument>
                                                                <ns2:mRID>341ddb3c-1847-4b2d-be19-e6c379cfbd53</ns2:mRID>
                                                                <ns2:revisionNumber>0.82</ns2:revisionNumber>
                                                                <ns2:type>Z04</ns2:type>
                                                                <ns2:createdDateTime>2025-07-29T06:26:48Z</ns2:createdDateTime>
                                                                <ns2:description>9bd0668f-cc19-40a8-99db-dc2cb2802b17</ns2:description>
                                                                <ns2:sender_MarketParticipant.mRID>
                                                                    <ns2:codingScheme>NDE</ns2:codingScheme>
                                                                    <ns2:value>sim</ns2:value>
                                                                </ns2:sender_MarketParticipant.mRID>
                                                                <ns2:sender_MarketParticipant.marketRole.type>A20</ns2:sender_MarketParticipant.marketRole.type>
                                                                <ns2:receiver_MarketParticipant.mRID>
                                                                    <ns2:codingScheme>NDE</ns2:codingScheme>
                                                                    <ns2:value>sim</ns2:value>
                                                                </ns2:receiver_MarketParticipant.mRID>
                                                                <ns2:receiver_MarketParticipant.marketRole.type>A50</ns2:receiver_MarketParticipant.marketRole.type>
                                                                <ns2:process.processType>A55</ns2:process.processType>
                                                                <ns2:period.timeInterval>
                                                                    <ns2:start>2021-01-01T00:00Z</ns2:start>
                                                                    <ns2:end>9999-12-31T00:00Z</ns2:end>
                                                                </ns2:period.timeInterval>
                                                                <ns2:PermissionList>
                                                                    <ns2:Permission>
                                                                        <ns2:permission.mRID>341ddb3c-1847-4b2d-be19-e6c379cfbd53</ns2:permission.mRID>
                                                                        <ns2:createdDateTime>2025-07-29T06:26:48Z</ns2:createdDateTime>
                                                                        <ns2:marketEvaluationPoint.mRID>
                                                                            <ns2:codingScheme>NDE</ns2:codingScheme>
                                                                            <ns2:value>id</ns2:value>
                                                                        </ns2:marketEvaluationPoint.mRID>
                                                                        <ns2:TimeSeriesList/>
                                                                        <ns2:MktActivityRecordList>
                                                                            <ns2:MktActivityRecord>
                                                                                <ns2:mRID>963e2eec-e875-4cfc-913d-1ea8e2d745d2</ns2:mRID>
                                                                                <ns2:createdDateTime>2025-07-29T06:26:48Z</ns2:createdDateTime>
                                                                                <ns2:description>FULFILLED</ns2:description>
                                                                                <ns2:type>sim</ns2:type>
                                                                                <ns2:status>Confirmed</ns2:status>
                                                                            </ns2:MktActivityRecord>
                                                                        </ns2:MktActivityRecordList>
                                                                        <ns2:ReasonList/>
                                                                    </ns2:Permission>
                                                                </ns2:PermissionList>
                                                            </ns2:Permission_MarketDocument>
                                                        </ns2:Permission_Envelope>
                                                    </PermissionMarketDocuments>
                                                    """
                                    )
                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the permission market documents by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the permission market documents by connectionId ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the permission market documents by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the permission market documents by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "N[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the permission market documents by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the permission market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the permission market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<PermissionMarketDocuments> permissionMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );

    @Operation(
            operationId = "GET accounting point data market document stream",
            summary = "GET accounting point data market document stream",
            description = "Get all new accounting point data market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(
                                    implementation = AccountingPointEnvelope.class
                            ),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                            {
                                              "MessageDocumentHeader": {
                                                "creationDateTime": "2025-07-30T06:07:59Z",
                                                "MessageDocumentHeader_MetaInformation": {
                                                  "connectionid": "1",
                                                  "permissionid": "1aa0ef01-98c3-4e5f-be51-af3d0ccbfffc",
                                                  "dataNeedid": "8685eed4-ab97-4c57-9409-76295792ee1c",
                                                  "dataType": "accounting-point-market-document",
                                                  "MessageDocumentHeader_Region": {
                                                    "connector": "cds",
                                                    "country": "NUS"
                                                  }
                                                }
                                              },
                                              "AccountingPoint_MarketDocument": {
                                                "mRID": "50e0a1b9-a10b-4114-b9de-9a1d0cb735aa",
                                                "revisionNumber": "0.82",
                                                "type": "B99",
                                                "createdDateTime": "2025-07-30T06:07:59Z",
                                                "description": "aacounting-point-market-document",
                                                "sender_MarketParticipant.mRID": {
                                                  "codingScheme": "NUS",
                                                  "value": "CDSC"
                                                },
                                                "sender_MarketParticipant.marketRole.type": "A26",
                                                "receiver_MarketParticipant.mRID": {
                                                  "codingScheme": "NUS",
                                                  "value": "meter-id"
                                                },
                                                "receiver_MarketParticipant.marketRole.type": "A13",
                                                "AccountingPointList": {
                                                  "AccountingPoint": [
                                                    {
                                                      "settlementMethod": null,
                                                      "mRID": {
                                                        "codingScheme": "NUS",
                                                        "value": "c40798cd-35b8-5d92-850f-aeb01ca27375"
                                                      },
                                                      "meterReadingResolution": null,
                                                      "gridAgreementType": null,
                                                      "name": null,
                                                      "administrativeStatus": null,
                                                      "flexibilityContract": null,
                                                      "resolution": null,
                                                      "commodity": "2",
                                                      "energyCommunity": null,
                                                      "direction": null,
                                                      "generationType": null,
                                                      "loadProfileType": null,
                                                      "supplyStatus": null,
                                                      "tariffClassDSO": null,
                                                      "ContractPartyList": {
                                                        "ContractParty": [
                                                          {
                                                            "contractPartyRole": "contractPartner",
                                                            "salutation": null,
                                                            "surName": "Doe",
                                                            "firstName": "Jane",
                                                            "companyName": null,
                                                            "Identification": null,
                                                            "dateOfBirth": null,
                                                            "dateOfDeath": null,
                                                            "companyRegisterNumber": null,
                                                            "VATnumber": null,
                                                            "email": null
                                                          }
                                                        ]
                                                      },
                                                      "AddressList": {
                                                        "Address": [
                                                          {
                                                            "addressRole": "delivery",
                                                            "postalCode": "11111",
                                                            "cityName": "Anytown",
                                                            "streetName": "Main St - HOME",
                                                            "buildingNumber": "123",
                                                            "staircaseNumber": null,
                                                            "floorNumber": null,
                                                            "doorNumber": null,
                                                            "addressSuffix": null
                                                          },
                                                          {
                                                            "addressRole": "invoice",
                                                            "postalCode": "11111",
                                                            "cityName": "Anytown",
                                                            "streetName": "Main St - HOME",
                                                            "buildingNumber": "123",
                                                            "staircaseNumber": null,
                                                            "floorNumber": null,
                                                            "doorNumber": null,
                                                            "addressSuffix": null
                                                          }
                                                        ]
                                                      },
                                                      "BillingData": null
                                                    }
                                                  ]
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<AccountingPointEnvelope>> accountingPointDataMdSSE();


    @Operation(
            operationId = "GET accounting point data market documents",
            summary = "GET accounting point data market documents",
            description = "Get all past accounting point data market documents",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AccountingPointEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                            [{
                                                      "MessageDocumentHeader": {
                                                        "creationDateTime": "2025-07-30T06:07:59Z",
                                                        "MessageDocumentHeader_MetaInformation": {
                                                          "connectionid": "1",
                                                          "permissionid": "1aa0ef01-98c3-4e5f-be51-af3d0ccbfffc",
                                                          "dataNeedid": "8685eed4-ab97-4c57-9409-76295792ee1c",
                                                          "dataType": "accounting-point-market-document",
                                                          "MessageDocumentHeader_Region": {
                                                            "connector": "cds",
                                                            "country": "NUS"
                                                          }
                                                        }
                                                      },
                                                      "AccountingPoint_MarketDocument": {
                                                        "mRID": "50e0a1b9-a10b-4114-b9de-9a1d0cb735aa",
                                                        "revisionNumber": "0.82",
                                                        "type": "B99",
                                                        "createdDateTime": "2025-07-30T06:07:59Z",
                                                        "description": "aacounting-point-market-document",
                                                        "sender_MarketParticipant.mRID": {
                                                          "codingScheme": "NUS",
                                                          "value": "CDSC"
                                                        },
                                                        "sender_MarketParticipant.marketRole.type": "A26",
                                                        "receiver_MarketParticipant.mRID": {
                                                          "codingScheme": "NUS",
                                                          "value": "meter-id"
                                                        },
                                                        "receiver_MarketParticipant.marketRole.type": "A13",
                                                        "AccountingPointList": {
                                                          "AccountingPoint": [
                                                            {
                                                              "settlementMethod": null,
                                                              "mRID": {
                                                                "codingScheme": "NUS",
                                                                "value": "c40798cd-35b8-5d92-850f-aeb01ca27375"
                                                              },
                                                              "meterReadingResolution": null,
                                                              "gridAgreementType": null,
                                                              "name": null,
                                                              "administrativeStatus": null,
                                                              "flexibilityContract": null,
                                                              "resolution": null,
                                                              "commodity": "2",
                                                              "energyCommunity": null,
                                                              "direction": null,
                                                              "generationType": null,
                                                              "loadProfileType": null,
                                                              "supplyStatus": null,
                                                              "tariffClassDSO": null,
                                                              "ContractPartyList": {
                                                                "ContractParty": [
                                                                  {
                                                                    "contractPartyRole": "contractPartner",
                                                                    "salutation": null,
                                                                    "surName": "Doe",
                                                                    "firstName": "Jane",
                                                                    "companyName": null,
                                                                    "Identification": null,
                                                                    "dateOfBirth": null,
                                                                    "dateOfDeath": null,
                                                                    "companyRegisterNumber": null,
                                                                    "VATnumber": null,
                                                                    "email": null
                                                                  }
                                                                ]
                                                              },
                                                              "AddressList": {
                                                                "Address": [
                                                                  {
                                                                    "addressRole": "delivery",
                                                                    "postalCode": "11111",
                                                                    "cityName": "Anytown",
                                                                    "streetName": "Main St - HOME",
                                                                    "buildingNumber": "123",
                                                                    "staircaseNumber": null,
                                                                    "floorNumber": null,
                                                                    "doorNumber": null,
                                                                    "addressSuffix": null
                                                                  },
                                                                  {
                                                                    "addressRole": "invoice",
                                                                    "postalCode": "11111",
                                                                    "cityName": "Anytown",
                                                                    "streetName": "Main St - HOME",
                                                                    "buildingNumber": "123",
                                                                    "staircaseNumber": null,
                                                                    "floorNumber": null,
                                                                    "doorNumber": null,
                                                                    "addressSuffix": null
                                                                  }
                                                                ]
                                                              },
                                                              "BillingData": null
                                                            }
                                                          ]
                                                        }
                                                      }
                                                    }]
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = PermissionMarketDocuments.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                                                    <AccountingPointDataMarketDocuments xmlns:ns3="htthttp://www.eddie.energy/AP/EDD04/20240422" >
                                                        <ns3:AccountingPoint_Envelope>
                                                            <ns3:MessageDocumentHeader>
                                                                <ns3:creationDateTime>2025-07-30T07:57:15Z</ns3:creationDateTime>
                                                                <ns3:MessageDocumentHeader_MetaInformation>
                                                                    <ns3:connectionid>1</ns3:connectionid>
                                                                    <ns3:permissionid>f08b5e93-3fbf-415b-8719-579e7c46c3ca</ns3:permissionid>
                                                                    <ns3:dataNeedid>8685eed4-ab97-4c57-9409-76295792ee1c</ns3:dataNeedid>
                                                                    <ns3:dataType>accounting-point-market-document</ns3:dataType>
                                                                    <ns3:MessageDocumentHeader_Region>
                                                                        <ns3:connector>cds</ns3:connector>
                                                                        <ns3:country>NUS</ns3:country>
                                                                    </ns3:MessageDocumentHeader_Region>
                                                                </ns3:MessageDocumentHeader_MetaInformation>
                                                            </ns3:MessageDocumentHeader>
                                                            <ns3:AccountingPoint_MarketDocument>
                                                                <ns3:mRID>436d9444-5f7f-4735-9c6c-b9ef624f43af</ns3:mRID>
                                                                <ns3:revisionNumber>0.82</ns3:revisionNumber>
                                                                <ns3:type>B99</ns3:type>
                                                                <ns3:createdDateTime>2025-07-30T07:57:15Z</ns3:createdDateTime>
                                                                <ns3:sender_MarketParticipant.mRID>
                                                                    <ns3:codingScheme>NUS</ns3:codingScheme>
                                                                    <ns3:value>CDSC</ns3:value>
                                                                </ns3:sender_MarketParticipant.mRID>
                                                                <ns3:sender_MarketParticipant.marketRole.type>A26</ns3:sender_MarketParticipant.marketRole.type>
                                                                <ns3:receiver_MarketParticipant.mRID>
                                                                    <ns3:codingScheme>NUS</ns3:codingScheme>
                                                                </ns3:receiver_MarketParticipant.mRID>
                                                                <ns3:receiver_MarketParticipant.marketRole.type>A13</ns3:receiver_MarketParticipant.marketRole.type>
                                                                <ns3:AccountingPointList>
                                                                    <ns3:AccountingPoint>
                                                                        <ns3:mRID>
                                                                            <ns3:codingScheme>NUS</ns3:codingScheme>
                                                                            <ns3:value>8a681951-2c0a-5ad9-8938-b539b59d0075</ns3:value>
                                                                        </ns3:mRID>
                                                                        <ns3:commodity>2</ns3:commodity>
                                                                        <ns3:ContractPartyList>
                                                                            <ns3:ContractParty>
                                                                                <ns3:contractPartyRole>contractPartner</ns3:contractPartyRole>
                                                                                <ns3:surName>Doe</ns3:surName>
                                                                                <ns3:firstName>Jane</ns3:firstName>
                                                                            </ns3:ContractParty>
                                                                        </ns3:ContractPartyList>
                                                                        <ns3:AddressList>
                                                                            <ns3:Address>
                                                                                <ns3:addressRole>delivery</ns3:addressRole>
                                                                                <ns3:postalCode>11111</ns3:postalCode>
                                                                                <ns3:cityName>Anytown</ns3:cityName>
                                                                                <ns3:streetName>Main St - HOME</ns3:streetName>
                                                                                <ns3:buildingNumber>123</ns3:buildingNumber>
                                                                            </ns3:Address>
                                                                            <ns3:Address>
                                                                                <ns3:addressRole>invoice</ns3:addressRole>
                                                                                <ns3:addressSuffix>123 Main St - HOME</ns3:addressSuffix>
                                                                            </ns3:Address>
                                                                        </ns3:AddressList>
                                                                    </ns3:AccountingPoint>
                                                                </ns3:AccountingPointList>
                                                            </ns3:AccountingPoint_MarketDocument>
                                                        </ns3:AccountingPoint_Envelope>
                                                    </AccountingPointDataMarketDocuments>
                                                    """
                                    )
                            )
                    }
            ),
            parameters = {
                    @Parameter(
                            name = "permissionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the accounting point data market documents by permission ID, use it only get the messages related to a single permission request",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "connectionId",
                            in = ParameterIn.QUERY,
                            description = "Filters the accounting point data market documents by connectionId ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "dataNeedId",
                            in = ParameterIn.QUERY,
                            description = "Filters the accounting point data market documents by the data need ID",
                            schema = @Schema(implementation = UUID.class)
                    ),
                    @Parameter(
                            name = "countryCode",
                            in = ParameterIn.QUERY,
                            description = "Filters the accounting point data market documents by the country, is a uppercase two letter country code",
                            schema = @Schema(implementation = String.class, pattern = "N[A-Z]{2}")
                    ),
                    @Parameter(
                            name = "regionConnectorId",
                            in = ParameterIn.QUERY,
                            description = "Filters the accounting point data market documents by the region connector",
                            schema = @Schema(implementation = String.class)
                    ),
                    @Parameter(
                            name = "from",
                            in = ParameterIn.QUERY,
                            description = "Filters the accounting point data market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
                    @Parameter(
                            name = "to",
                            in = ParameterIn.QUERY,
                            description = "Filters the accounting point data market documents by the time they were received",
                            schema = @Schema(implementation = ZonedDateTime.class)
                    ),
            }
    )
    ResponseEntity<AccountingPointDataMarketDocuments> accountingPointDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    );


    @Operation(
            operationId = "POST termination market document",
            summary = "POST termination market document stream",
            description = "POST a termination market document, that will terminate an already accepted permission request",
            method = "POST",
            responses = @ApiResponse(responseCode = "202"),
            requestBody = @RequestBody(
                    description = "The body indicates which permission request for which region connector needs to be terminated",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PermissionEnvelope.class),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            """
                                                    {
                                                      "Permission_MarketDocument": {
                                                        "mRID": "{{permissionId}}",
                                                        "type": "Z01",
                                                        "PermissionList": {
                                                          "Permission": [
                                                            {
                                                              "MktActivityRecordList": {
                                                                "MktActivityRecord": [
                                                                  {
                                                                    "type": "{{region-connector-id}}"
                                                                  }
                                                                ]
                                                              },
                                                              "ReasonList": {
                                                                "Reason": [
                                                                  {
                                                                    "code": "Z03"
                                                                  }
                                                                ]
                                                              }
                                                            }
                                                          ]
                                                        }
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
                                            """
                                                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                                                    <Permission_Envelope xmlns="http://www.eddie.energy/Consent/EDD02/20240125">
                                                        <Permission_MarketDocument>
                                                            <mRID>{{permissionId}}</mRID>
                                                            <type>Z01</type>
                                                            <PermissionList>
                                                                <Permission>
                                                                    <MktActivityRecordList>
                                                                        <MktActivityRecord>
                                                                            <type>{{region-connector-id}}</type>
                                                                        </MktActivityRecord>
                                                                    </MktActivityRecordList>
                                                                    <ReasonList>
                                                                        <Reason>
                                                                            <code>Z03</code>
                                                                        </Reason>
                                                                    </ReasonList>
                                                                </Permission>
                                                            </PermissionList>
                                                        </Permission_MarketDocument>
                                                    </Permission_Envelope>
                                                    """
                                    )
                            ),
                    }
            )
    )
    ResponseEntity<Void> terminationMd(PermissionEnvelope permissionEnvelope);
}
