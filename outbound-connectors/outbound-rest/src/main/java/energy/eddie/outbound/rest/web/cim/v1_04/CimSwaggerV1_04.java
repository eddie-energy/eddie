package energy.eddie.outbound.rest.web.cim.v1_04;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.dto.NearRealTimeDataMarketDocuments;
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

@SuppressWarnings({
        "OptionalUsedAsFieldOrParameterType", 
        "unused",
        "java:S101" // Names shouldn't contain underscores, but this is required to not have bean name clashes with the other CimSwagger
})
@Tag(name = "CIM v1.04 Documents", description = "Provides endpoints for CIM v1.04 documents, such as near real-time data market documents.")
public interface CimSwaggerV1_04 {
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
