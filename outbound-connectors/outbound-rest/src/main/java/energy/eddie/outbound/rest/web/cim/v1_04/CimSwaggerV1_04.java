package energy.eddie.outbound.rest.web.cim.v1_04;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.dto.NearRealTimeDataMarketDocuments;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocumentsV1_04;
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
@Tag(name = "CIM v1.04 Documents", description = "Provides endpoints for CIM v1.04 documents, such as validated historical data market documents.")
public interface CimSwaggerV1_04 {

    @Operation(
            operationId = "GET validated historical data market document stream v1.04",
            summary = "GET validated historical data market document stream v1.04",
            description = "Get all new validated historical data market documents as Server Sent Events",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(mediaType = "text/event-stream",
                            schema = @Schema(implementation = VHDEnvelope.class),
                            examples = @ExampleObject(
                                    // language=JSON
                                    value = """
                                            {
                                              "messageDocumentHeader.creationDateTime": "2025-11-11T08:33:39Z",
                                              "messageDocumentHeader.metaInformation.connectionId": "1",
                                              "messageDocumentHeader.metaInformation.dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                              "messageDocumentHeader.metaInformation.documentType": "validated-historical-data-market-document",
                                              "messageDocumentHeader.metaInformation.permissionId": "926a6034-f1ba-46a8-8d4b-1da8244ec6cb",
                                              "messageDocumentHeader.metaInformation.region.connector": "sim",
                                              "messageDocumentHeader.metaInformation.region.country": "DE",
                                              "MarketDocument": {
                                                "mRID": "ff50d697-8fee-4b3e-9729-917d6fb766f8",
                                                "revisionNumber": "104",
                                                "description": null,
                                                "type": "A45",
                                                "createdDateTime": "2025-11-11T08:33:39Z",
                                                "sender_MarketParticipant.mRID": {
                                                  "value": "sim",
                                                  "codingScheme": "A01"
                                                },
                                                "sender_MarketParticipant.marketRole.type": "A26",
                                                "receiver_MarketParticipant.mRID": {
                                                  "value": "sim",
                                                  "codingScheme": "NAT"
                                                },
                                                "receiver_MarketParticipant.marketRole.type": "A13",
                                                "period.timeInterval": {
                                                  "start": "2024-12-30T09:49Z",
                                                  "end": "2024-12-30T10:04Z"
                                                },
                                                "process.processType": "A16",
                                                "TimeSeries": [
                                                  {
                                                    "version": "1",
                                                    "mRID": "a6660ef1-891c-400e-bbb7-ac105d1fdbb7",
                                                    "businessType": "A04",
                                                    "product": "8716867000030",
                                                    "dateAndOrTime.dateTime": null,
                                                    "energy_Measurement_Unit.name": "WTT",
                                                    "flowDirection.direction": "A02",
                                                    "Period": [
                                                      {
                                                        "resolution": "P0Y0M0DT0H15M0.000S",
                                                        "timeInterval": {
                                                          "start": "2024-12-30T09:49Z",
                                                          "end": "2024-12-30T10:04Z"
                                                        },
                                                        "Point": [
                                                          {
                                                            "position": 1,
                                                            "energy_Quantity.quantity": 10.0,
                                                            "energy_Quantity.type": null,
                                                            "energy_Quantity.quality": "A04",
                                                            "energyQuality_Quantity.quantity": null,
                                                            "energyQuality_Quantity.type": null,
                                                            "energyQuality_Quantity.quality": null
                                                          }
                                                        ],
                                                        "reason.code": "999",
                                                        "reason.text": null
                                                      }
                                                    ],
                                                    "registeredResource.mRID": null,
                                                    "registeredResource.name": null,
                                                    "registeredResource.description": null,
                                                    "registeredResource.fuel.fuel": null,
                                                    "registeredResource.location.mRID": null,
                                                    "registeredResource.location.type": null,
                                                    "registeredResource.location.coordinateSystem.crsUrn": null,
                                                    "registeredResource.location.positionPoints.sequenceNumber": null,
                                                    "registeredResource.location.positionPoints.xPosition": null,
                                                    "registeredResource.location.positionPoints.yPosition": null,
                                                    "registeredResource.location.positionPoints.zPosition": null,
                                                    "registeredResource.pSRType.psrType": null,
                                                    "marketEvaluationPoint.mRID": {
                                                      "value": "mid",
                                                      "codingScheme": "NFR"
                                                    },
                                                    "marketEvaluationPoint.meterReadings.mRID": null,
                                                    "marketEvaluationPoint.meterReadings.readings.mRID": null,
                                                    "marketEvaluationPoint.meterReadings.readings.readingType.accumulation": null,
                                                    "marketEvaluationPoint.meterReadings.readings.readingType.aggregate": "26",
                                                    "marketEvaluationPoint.meterReadings.readings.readingType.commodity": "0",
                                                    "marketEvaluationPoint.usagePointLocation.geoInfoReference": null,
                                                    "reason.code": "999",
                                                    "reason.text": null,
                                                    "energyQuality_Measurement_Unit.name": null
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<Flux<VHDEnvelope>> validatedHistoricalDataMdSSE();


    @Operation(
            operationId = "GET validated historical data market documents v1.04",
            summary = "GET validated historical data market documents v1.04",
            description = "Get all past validated historical data market documents",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = VHDEnvelope.class)),
                                    examples = @ExampleObject(
                                            // language=JSON
                                            value = """
                                                    [
                                                      {
                                                        "messageDocumentHeader.creationDateTime": "2025-11-11T08:33:39Z",
                                                        "messageDocumentHeader.metaInformation.connectionId": "1",
                                                        "messageDocumentHeader.metaInformation.dataNeedId": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                                                        "messageDocumentHeader.metaInformation.documentType": "validated-historical-data-market-document",
                                                        "messageDocumentHeader.metaInformation.permissionId": "926a6034-f1ba-46a8-8d4b-1da8244ec6cb",
                                                        "messageDocumentHeader.metaInformation.region.connector": "sim",
                                                        "messageDocumentHeader.metaInformation.region.country": "DE",
                                                        "MarketDocument": {
                                                          "mRID": "ff50d697-8fee-4b3e-9729-917d6fb766f8",
                                                          "revisionNumber": "104",
                                                          "description": null,
                                                          "type": "A45",
                                                          "createdDateTime": "2025-11-11T08:33:39Z",
                                                          "sender_MarketParticipant.mRID": {
                                                            "value": "sim",
                                                            "codingScheme": "A01"
                                                          },
                                                          "sender_MarketParticipant.marketRole.type": "A26",
                                                          "receiver_MarketParticipant.mRID": {
                                                            "value": "sim",
                                                            "codingScheme": "NAT"
                                                          },
                                                          "receiver_MarketParticipant.marketRole.type": "A13",
                                                          "period.timeInterval": {
                                                            "start": "2024-12-30T09:49Z",
                                                            "end": "2024-12-30T10:04Z"
                                                          },
                                                          "process.processType": "A16",
                                                          "TimeSeries": [
                                                            {
                                                              "version": "1",
                                                              "mRID": "a6660ef1-891c-400e-bbb7-ac105d1fdbb7",
                                                              "businessType": "A04",
                                                              "product": "8716867000030",
                                                              "dateAndOrTime.dateTime": null,
                                                              "energy_Measurement_Unit.name": "WTT",
                                                              "flowDirection.direction": "A02",
                                                              "Period": [
                                                                {
                                                                  "resolution": "P0Y0M0DT0H15M0.000S",
                                                                  "timeInterval": {
                                                                    "start": "2024-12-30T09:49Z",
                                                                    "end": "2024-12-30T10:04Z"
                                                                  },
                                                                  "Point": [
                                                                    {
                                                                      "position": 1,
                                                                      "energy_Quantity.quantity": 10.0,
                                                                      "energy_Quantity.type": null,
                                                                      "energy_Quantity.quality": "A04",
                                                                      "energyQuality_Quantity.quantity": null,
                                                                      "energyQuality_Quantity.type": null,
                                                                      "energyQuality_Quantity.quality": null
                                                                    }
                                                                  ],
                                                                  "reason.code": "999",
                                                                  "reason.text": null
                                                                }
                                                              ],
                                                              "registeredResource.mRID": null,
                                                              "registeredResource.name": null,
                                                              "registeredResource.description": null,
                                                              "registeredResource.fuel.fuel": null,
                                                              "registeredResource.location.mRID": null,
                                                              "registeredResource.location.type": null,
                                                              "registeredResource.location.coordinateSystem.crsUrn": null,
                                                              "registeredResource.location.positionPoints.sequenceNumber": null,
                                                              "registeredResource.location.positionPoints.xPosition": null,
                                                              "registeredResource.location.positionPoints.yPosition": null,
                                                              "registeredResource.location.positionPoints.zPosition": null,
                                                              "registeredResource.pSRType.psrType": null,
                                                              "marketEvaluationPoint.mRID": {
                                                                "value": "mid",
                                                                "codingScheme": "NFR"
                                                              },
                                                              "marketEvaluationPoint.meterReadings.mRID": null,
                                                              "marketEvaluationPoint.meterReadings.readings.mRID": null,
                                                              "marketEvaluationPoint.meterReadings.readings.readingType.accumulation": null,
                                                              "marketEvaluationPoint.meterReadings.readings.readingType.aggregate": "26",
                                                              "marketEvaluationPoint.meterReadings.readings.readingType.commodity": "0",
                                                              "marketEvaluationPoint.usagePointLocation.geoInfoReference": null,
                                                              "reason.code": "999",
                                                              "reason.text": null,
                                                              "energyQuality_Measurement_Unit.name": null
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
                                    schema = @Schema(implementation = ValidatedHistoricalDataMarketDocumentsV1_04.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                                                    <VHDEnvelopes xmlns:ns5="https//eddie.energy/CIM/VHD_v1.04"                                                     >
                                                      <ns5:VHD_Envelope>
                                                        <ns5:messageDocumentHeader.creationDateTime>2025-11-11T08:33:39Z</ns5:messageDocumentHeader.creationDateTime>
                                                        <ns5:messageDocumentHeader.metaInformation.connectionId>1</ns5:messageDocumentHeader.metaInformation.connectionId>
                                                        <ns5:messageDocumentHeader.metaInformation.dataNeedId>9bd0668f-cc19-40a8-99db-dc2cb2802b17</ns5:messageDocumentHeader.metaInformation.dataNeedId>
                                                        <ns5:messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</ns5:messageDocumentHeader.metaInformation.documentType>
                                                        <ns5:messageDocumentHeader.metaInformation.permissionId>926a6034-f1ba-46a8-8d4b-1da8244ec6cb</ns5:messageDocumentHeader.metaInformation.permissionId>
                                                        <ns5:messageDocumentHeader.metaInformation.region.connector>sim</ns5:messageDocumentHeader.metaInformation.region.connector>
                                                        <ns5:messageDocumentHeader.metaInformation.region.country>DE</ns5:messageDocumentHeader.metaInformation.region.country>
                                                        <ns5:MarketDocument>
                                                          <ns5:mRID>ff50d697-8fee-4b3e-9729-917d6fb766f8</ns5:mRID>
                                                          <ns5:revisionNumber>104</ns5:revisionNumber>
                                                          <ns5:type>A45</ns5:type>
                                                          <ns5:createdDateTime>2025-11-11T08:33:39Z</ns5:createdDateTime>
                                                          <ns5:sender_MarketParticipant.mRID codingScheme="A01">sim</ns5:sender_MarketParticipant.mRID>
                                                          <ns5:sender_MarketParticipant.marketRole.type>A26</ns5:sender_MarketParticipant.marketRole.type>
                                                          <ns5:receiver_MarketParticipant.mRID codingScheme="NAT">sim</ns5:receiver_MarketParticipant.mRID>
                                                          <ns5:receiver_MarketParticipant.marketRole.type>A13</ns5:receiver_MarketParticipant.marketRole.type>
                                                          <ns5:period.timeInterval>
                                                            <ns5:start>2024-12-30T09:49Z</ns5:start>
                                                            <ns5:end>2024-12-30T10:04Z</ns5:end>
                                                          </ns5:period.timeInterval>
                                                          <ns5:process.processType>A16</ns5:process.processType>
                                                          <ns5:TimeSeries>
                                                            <ns5:version>1</ns5:version>
                                                            <ns5:mRID>a6660ef1-891c-400e-bbb7-ac105d1fdbb7</ns5:mRID>
                                                            <ns5:businessType>A04</ns5:businessType>
                                                            <ns5:product>8716867000030</ns5:product>
                                                            <ns5:energy_Measurement_Unit.name>WTT</ns5:energy_Measurement_Unit.name>
                                                            <ns5:flowDirection.direction>A02</ns5:flowDirection.direction>
                                                            <ns5:Period>
                                                              <ns5:resolution>P0Y0M0DT0H15M0.000S</ns5:resolution>
                                                              <ns5:timeInterval>
                                                                <ns5:start>2024-12-30T09:49Z</ns5:start>
                                                                <ns5:end>2024-12-30T10:04Z</ns5:end>
                                                              </ns5:timeInterval>
                                                              <ns5:Point>
                                                                <ns5:position>1</ns5:position>
                                                                <ns5:energy_Quantity.quantity>10.0</ns5:energy_Quantity.quantity>
                                                                <ns5:energy_Quantity.quality>A04</ns5:energy_Quantity.quality>
                                                              </ns5:Point>
                                                              <ns5:reason.code>999</ns5:reason.code>
                                                            </ns5:Period>
                                                            <ns5:marketEvaluationPoint.mRID codingScheme="NFR">mid</ns5:marketEvaluationPoint.mRID>
                                                            <ns5:marketEvaluationPoint.meterReadings.readings.readingType.aggregate>26</ns5:marketEvaluationPoint.meterReadings.readings.readingType.aggregate>
                                                            <ns5:marketEvaluationPoint.meterReadings.readings.readingType.commodity>0</ns5:marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                                                            <ns5:reason.code>999</ns5:reason.code>
                                                          </ns5:TimeSeries>
                                                        </ns5:MarketDocument>
                                                      </ns5:VHD_Envelope>
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
    ResponseEntity<ValidatedHistoricalDataMarketDocumentsV1_04> validatedHistoricalDataMd(
            Optional<String> permissionId,
            Optional<String> connectionId,
            Optional<String> dataNeedId,
            Optional<String> countryCode,
            Optional<String> regionConnectorId,
            Optional<ZonedDateTime> from,
            Optional<ZonedDateTime> to
    );


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
                                                "messageDocumentHeader.creationDateTime": "2025-07-01T09:44:00.00040249Z",
                                                "messageDocumentHeader.metaInformation.connectionId": "3",
                                                "messageDocumentHeader.metaInformation.dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
                                                "messageDocumentHeader.metaInformation.documentType": "near-real-time-market-document",
                                                "messageDocumentHeader.metaInformation.permissionId": "150cfd97-64bb-402b-838f-57f8605713b7",
                                                "messageDocumentHeader.metaInformation.finalCustomerId": "008cf1d6-e118-45a8-bc17-a331dfc57e77",
                                                "messageDocumentHeader.metaInformation.asset": "CONNECTION-AGREEMENT-POINT",
                                                "messageDocumentHeader.metaInformation.dataSourceId": "5eef407d-d14f-49d4-b61a-769a20caa540",
                                                "messageDocumentHeader.metaInformation.regionConnector": "aiida",
                                                "messageDocumentHeader.metaInformation.regionCountry": "AT",
                                                "marketDocument": {
                                                  "mrid": "bff481d5-edd8-4602-9c54-838b386ab4dd",
                                                  "createdDateTime": "2025-07-01T09:44:00.00032307Z",
                                                  "timeSeries": [
                                                    {
                                                      "version": "1.0",
                                                      "registeredResourceMRID": {
                                                        "value": "5eef407d-d14f-49d4-b61a-769a20caa540",
                                                        "codingScheme": "NAT"
                                                      },
                                                      "dateAndOrTimeDateTime": "2025-07-01T07:43:59.073747585Z",
                                                      "quantities": [
                                                        {
                                                          "quantity": 25,
                                                          "type": "0",
                                                          "quality": "AS_PROVIDED"
                                                        },
                                                        {
                                                          "quantity": 1750,
                                                          "type": "2",
                                                          "quality": "AS_PROVIDED"
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
                                                      [{
                                                         "messageDocumentHeader.creationDateTime": "2025-07-01T09:44:00.00040249Z",
                                                         "messageDocumentHeader.metaInformation.connectionId": "3",
                                                         "messageDocumentHeader.metaInformation.dataNeedId": "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
                                                         "messageDocumentHeader.metaInformation.documentType": "near-real-time-market-document",
                                                         "messageDocumentHeader.metaInformation.permissionId": "150cfd97-64bb-402b-838f-57f8605713b7",
                                                         "messageDocumentHeader.metaInformation.finalCustomerId": "008cf1d6-e118-45a8-bc17-a331dfc57e77",
                                                         "messageDocumentHeader.metaInformation.asset": "CONNECTION-AGREEMENT-POINT",
                                                         "messageDocumentHeader.metaInformation.dataSourceId": "5eef407d-d14f-49d4-b61a-769a20caa540",
                                                         "messageDocumentHeader.metaInformation.regionConnector": "aiida",
                                                         "messageDocumentHeader.metaInformation.regionCountry": "AT",
                                                         "marketDocument": {
                                                           "mrid": "bff481d5-edd8-4602-9c54-838b386ab4dd",
                                                           "createdDateTime": "2025-07-01T09:44:00.00032307Z",
                                                           "timeSeries": [
                                                             {
                                                               "version": "1.0",
                                                               "registeredResourceMRID": {
                                                                 "value": "5eef407d-d14f-49d4-b61a-769a20caa540",
                                                                 "codingScheme": "NAT"
                                                               },
                                                               "dateAndOrTimeDateTime": "2025-07-01T07:43:59.073747585Z",
                                                               "quantities": [
                                                                 {
                                                                   "quantity": 25,
                                                                   "type": "0",
                                                                   "quality": "AS_PROVIDED"
                                                                 },
                                                                 {
                                                                   "quantity": 1750,
                                                                   "type": "2",
                                                                   "quality": "AS_PROVIDED"
                                                                 }
                                                               ]
                                                             }
                                                           ]
                                                         }
                                                       }]
                                                    """
                                    )
                            ),
                            @Content(
                                    mediaType = "application/xml",
                                    schema = @Schema(implementation = NearRealTimeDataMarketDocuments.class),
                                    examples = @ExampleObject(
                                            // language=XML
                                            value = """
                                                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                                                    <NearRealTimeDataMarketDocuments xmlns:ns2="http://www.eddie.energy/RTD/EDD01/20240614">
                                                        <RTD_Envelope>
                                                            <messageDocumentHeader.creationDateTime>2025-07-01T09:44:00.00040249Z</messageDocumentHeader.creationDateTime>
                                                            <messageDocumentHeader.metaInformation.connectionId>3</messageDocumentHeader.metaInformation.connectionId>
                                                            <messageDocumentHeader.metaInformation.dataNeedId>5dc71d7e-e8cd-4403-a3a8-d3c095c97a84</messageDocumentHeader.metaInformation.dataNeedId>
                                                            <messageDocumentHeader.metaInformation.documentType>near-real-time-market-document</messageDocumentHeader.metaInformation.documentType>
                                                            <messageDocumentHeader.metaInformation.permissionId>150cfd97-64bb-402b-838f-57f8605713b7</messageDocumentHeader.metaInformation.permissionId>
                                                            <messageDocumentHeader.metaInformation.finalCustomerId>008cf1d6-e118-45a8-bc17-a331dfc57e77</messageDocumentHeader.metaInformation.finalCustomerId>
                                                            <messageDocumentHeader.metaInformation.asset>CONNECTION-AGREEMENT-POINT</messageDocumentHeader.metaInformation.asset>
                                                            <messageDocumentHeader.metaInformation.dataSourceId>5eef407d-d14f-49d4-b61a-769a20caa540</messageDocumentHeader.metaInformation.dataSourceId>
                                                            <messageDocumentHeader.metaInformation.regionConnector>aiida</messageDocumentHeader.metaInformation.regionConnector>
                                                            <messageDocumentHeader.metaInformation.regionCountry>AT</messageDocumentHeader.metaInformation.regionCountry>
                                                            <marketDocument>
                                                                <mrid>bff481d5-edd8-4602-9c54-838b386ab4dd</mrid>
                                                                <createdDateTime>2025-07-01T09:44:00.00032307Z</createdDateTime>
                                                                <timeSeries>
                                                                    <version>1.0</version>
                                                                    <registeredResourceMRID>
                                                                        <value>5eef407d-d14f-49d4-b61a-769a20caa540</value>
                                                                        <codingScheme>NAT</codingScheme>
                                                                    </registeredResourceMRID>
                                                                    <dateAndOrTimeDateTime>2025-07-01T07:43:59.073747585Z</dateAndOrTimeDateTime>
                                                                    <quantities>
                                                                        <quantity>25</quantity>
                                                                        <type>0</type>
                                                                        <quality>AS_PROVIDED</quality>
                                                                    </quantities>
                                                                    <quantities>
                                                                        <quantity>1750</quantity>
                                                                        <type>2</type>
                                                                        <quality>AS_PROVIDED</quality>
                                                                    </quantities>
                                                                </timeSeries>
                                                            </marketDocument>
                                                        </RTD_Envelope>
                                                    </NearRealTimeDataMarketDocuments>
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
                            schema = @Schema(implementation = UUID.class)
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
