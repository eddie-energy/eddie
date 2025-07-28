package energy.eddie.outbound.rest.web.cim.v0_82;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocuments;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
@Tag(name = "CIM v0.82 Documents", description = "Provides endpoints for CIM v0.82 documents, such as validated historical data market documents.")
public interface CimSwagger {

    @Operation(
            operationId = "GET validated historical data market document stream stream",
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
            operationId = "GET validated historical data market document stream stream",
            summary = "GET validated historical data market document stream",
            description = "Get all new validated historical data market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ValidatedHistoricalDataMarketDocuments.class),
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
}
