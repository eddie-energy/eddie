package energy.eddie.regionconnector.be.fluvius.provider.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xmlunit.builder.DiffBuilder;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntermediateValidatedHistoricalDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();
    @Mock
    private DataNeedsService dataNeedsService;

    IntermediateValidatedHistoricalDocumentTest() throws SerdeInitializationException {}

    @Test
    void givenElectricityDailyResponseData_whenToVhd_thenReturnVhd() throws SerializationException {
        // Given
        when(dataNeedsService.getById("dnid")).thenReturn(new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.P1D,
                Granularity.P1D
        ));
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <ns:VHD_Envelope xmlns:ns="https//eddie.energy/CIM/VHD_v1.04">
                    <ns:messageDocumentHeader.creationDateTime>ignored</ns:messageDocumentHeader.creationDateTime>
                    <ns:messageDocumentHeader.metaInformation.connectionId>cid</ns:messageDocumentHeader.metaInformation.connectionId>
                    <ns:messageDocumentHeader.metaInformation.dataNeedId>dnid</ns:messageDocumentHeader.metaInformation.dataNeedId>
                    <ns:messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</ns:messageDocumentHeader.metaInformation.documentType>
                    <ns:messageDocumentHeader.metaInformation.permissionId>pid</ns:messageDocumentHeader.metaInformation.permissionId>
                    <ns:messageDocumentHeader.metaInformation.region.connector>be-fluvius</ns:messageDocumentHeader.metaInformation.region.connector>
                    <ns:messageDocumentHeader.metaInformation.region.country>NBE</ns:messageDocumentHeader.metaInformation.region.country>
                    <ns:MarketDocument>
                        <ns:mRID>anything</ns:mRID>
                        <ns:revisionNumber>104</ns:revisionNumber>
                        <ns:description>1</ns:description>
                        <ns:type>A45</ns:type>
                        <ns:createdDateTime>ignored</ns:createdDateTime>
                        <ns:sender_MarketParticipant.marketRole.type>A26</ns:sender_MarketParticipant.marketRole.type>
                        <ns:receiver_MarketParticipant.marketRole.type>A13</ns:receiver_MarketParticipant.marketRole.type>
                        <ns:sender_MarketParticipant.mRID codingScheme="NBE">Fluvius</ns:sender_MarketParticipant.mRID>
                        <ns:receiver_MarketParticipant.mRID codingScheme="NBE">client-id</ns:receiver_MarketParticipant.mRID>
                        <ns:period.timeInterval>
                            <ns:start>2025-01-01T00:00Z</ns:start>
                            <ns:end>2025-01-02T00:00Z</ns:end>
                        </ns:period.timeInterval>
                        <ns:process.processType>A16</ns:process.processType>
                        <ns:TimeSeries>
                            <ns:mRID>ignored</ns:mRID>
                            <ns:version>2</ns:version>
                            <ns:businessType>A07</ns:businessType>
                            <ns:product>8716867000030</ns:product>
                            <ns:energy_Measurement_Unit.name>KWH</ns:energy_Measurement_Unit.name>
                            <ns:flowDirection.direction>A03</ns:flowDirection.direction>
                            <ns:Period>
                                <ns:resolution>P0Y0M1DT0H0M0.000S</ns:resolution>
                                <ns:timeInterval>
                                    <ns:start>2025-01-01T00:00Z</ns:start>
                                    <ns:end>2025-01-02T00:00Z</ns:end>
                                </ns:timeInterval>
                                <ns:Point>
                                    <ns:position>1</ns:position>
                                    <ns:energy_Quantity.quantity>10.0</ns:energy_Quantity.quantity>
                                    <ns:energy_Quantity.quality>A03</ns:energy_Quantity.quality>
                                </ns:Point>
                            </ns:Period>
                            <ns:marketEvaluationPoint.meterReadings.readings.readingType.commodity>2</ns:marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                            <ns:marketEvaluationPoint.meterReadings.mRID codingScheme="NBE">mid</ns:marketEvaluationPoint.meterReadings.mRID>
                            <ns:reason.code>999</ns:reason.code>
                        </ns:TimeSeries>
                    </ns:MarketDocument>
                </ns:VHD_Envelope>
                """;
        var config = new FluviusOAuthConfiguration("token", "client-id", "secret", "tenant", "scope");
        var pr = DefaultFluviusPermissionRequestBuilder
                .create()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .granularity(Granularity.P1D)
                .build();
        var timestampStart = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var timestampEnd = OffsetDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        var data = new GetEnergyResponseModelApiDataResponse()
                .metaData(
                        new ApiMetaData()
                                .version("2")
                )
                .data(
                        new GetEnergyResponseModel()
                                .fetchTime(OffsetDateTime.now(ZoneOffset.UTC))
                                .addElectricityMetersItem(
                                        new ElectricityMeterResponseModel()
                                                .seqNumber(1)
                                                .meterID("mid")
                                                .addDailyEnergyItem(
                                                        new EDailyEnergyItemResponseModel()
                                                                .timestampStart(timestampStart)
                                                                .timestampEnd(timestampEnd)
                                                                .addMeasurementItem(
                                                                        new EMeasurementItemResponseModel()
                                                                                .unit("kwh")
                                                                                .offtakeDayValue(10.0)
                                                                                .injectionDayValue(5.0)
                                                                                .offtakeNightValue(10.0)
                                                                                .injectionNightValue(5.0)
                                                                                .offtakeDayValidationState("READ")
                                                                                .injectionDayValidationState("READ")
                                                                                .offtakeNightValidationState("READ")
                                                                                .injectionNightValidationState("EST")
                                                                )
                                                )
                                )
                );
        var id = new IntermediateValidatedHistoricalDocument(
                config,
                new IdentifiableMeteringData(pr, data),
                dataNeedsService
        );

        // When
        var vhds = id.toVHD();

        // Then
        var ignoredNames = Set.of(
                "messageDocumentHeader.creationDateTime",
                "createdDateTime",
                "dateAndOrTime.dateTime",
                "mRID"
        );
        var res = vhds.getFirst();
        var bytes = serde.serialize(res);
        var testXml = new String(bytes, StandardCharsets.UTF_8);
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(testXml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> ignoredNames.stream().noneMatch(node.getNodeName()::endsWith))
                                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
        assertTrue(XmlValidator.validateV104ValidatedHistoricalDataMarketDocument(bytes));
    }

    @Test
    void givenElectricityQuarterHourlyResponseData_whenToVhd_thenReturnVhd() throws SerializationException {
        // Given
        when(dataNeedsService.getById("dnid")).thenReturn(new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.P1D
        ));
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <ns:VHD_Envelope xmlns:ns="https//eddie.energy/CIM/VHD_v1.04">
                    <ns:messageDocumentHeader.creationDateTime>ignored</ns:messageDocumentHeader.creationDateTime>
                    <ns:messageDocumentHeader.metaInformation.connectionId>cid</ns:messageDocumentHeader.metaInformation.connectionId>
                    <ns:messageDocumentHeader.metaInformation.dataNeedId>dnid</ns:messageDocumentHeader.metaInformation.dataNeedId>
                    <ns:messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</ns:messageDocumentHeader.metaInformation.documentType>
                    <ns:messageDocumentHeader.metaInformation.permissionId>pid</ns:messageDocumentHeader.metaInformation.permissionId>
                    <ns:messageDocumentHeader.metaInformation.region.connector>be-fluvius</ns:messageDocumentHeader.metaInformation.region.connector>
                    <ns:messageDocumentHeader.metaInformation.region.country>NBE</ns:messageDocumentHeader.metaInformation.region.country>
                    <ns:MarketDocument>
                        <ns:mRID>anything</ns:mRID>
                        <ns:revisionNumber>104</ns:revisionNumber>
                        <ns:description>1</ns:description>
                        <ns:type>A45</ns:type>
                        <ns:createdDateTime>ignored</ns:createdDateTime>
                        <ns:sender_MarketParticipant.marketRole.type>A26</ns:sender_MarketParticipant.marketRole.type>
                        <ns:receiver_MarketParticipant.marketRole.type>A13</ns:receiver_MarketParticipant.marketRole.type>
                        <ns:sender_MarketParticipant.mRID codingScheme="NBE">Fluvius</ns:sender_MarketParticipant.mRID>
                        <ns:receiver_MarketParticipant.mRID codingScheme="NBE">client-id</ns:receiver_MarketParticipant.mRID>
                        <ns:period.timeInterval>
                            <ns:start>2025-01-01T00:00Z</ns:start>
                            <ns:end>2025-01-01T00:15Z</ns:end>
                        </ns:period.timeInterval>
                        <ns:process.processType>A16</ns:process.processType>
                        <ns:TimeSeries>
                            <ns:mRID>ignored</ns:mRID>
                            <ns:version>2</ns:version>
                            <ns:businessType>A07</ns:businessType>
                            <ns:product>8716867000030</ns:product>
                            <ns:energy_Measurement_Unit.name>KWH</ns:energy_Measurement_Unit.name>
                            <ns:flowDirection.direction>A03</ns:flowDirection.direction>
                            <ns:Period>
                                <ns:resolution>P0Y0M0DT0H15M0.000S</ns:resolution>
                                <ns:timeInterval>
                                    <ns:start>2025-01-01T00:00Z</ns:start>
                                    <ns:end>2025-01-01T00:15Z</ns:end>
                                </ns:timeInterval>
                                <ns:Point>
                                    <ns:position>1</ns:position>
                                    <ns:energy_Quantity.quantity>10.0</ns:energy_Quantity.quantity>
                                    <ns:energy_Quantity.quality>A04</ns:energy_Quantity.quality>
                                </ns:Point>
                            </ns:Period>
                            <ns:marketEvaluationPoint.meterReadings.readings.readingType.commodity>2</ns:marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                            <ns:marketEvaluationPoint.meterReadings.mRID codingScheme="NBE">mid</ns:marketEvaluationPoint.meterReadings.mRID>
                            <ns:reason.code>999</ns:reason.code>
                        </ns:TimeSeries>
                    </ns:MarketDocument>
                </ns:VHD_Envelope>
                """;
        var config = new FluviusOAuthConfiguration("token", "client-id", "secret", "tenant", "scope");
        var pr = DefaultFluviusPermissionRequestBuilder
                .create()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .granularity(Granularity.PT15M)
                .build();
        var timestampStart = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var timestampEnd = OffsetDateTime.of(2025, 1, 1, 0, 15, 0, 0, ZoneOffset.UTC);
        var data = new GetEnergyResponseModelApiDataResponse()
                .metaData(
                        new ApiMetaData()
                                .version("2")
                )
                .data(
                        new GetEnergyResponseModel()
                                .fetchTime(OffsetDateTime.now(ZoneOffset.UTC))
                                .addElectricityMetersItem(
                                        new ElectricityMeterResponseModel()
                                                .seqNumber(1)
                                                .meterID("mid")
                                                .addQuarterHourlyEnergyItem(
                                                        new EQuarterHourlyEnergyItemResponseModel()
                                                                .timestampStart(timestampStart)
                                                                .timestampEnd(timestampEnd)
                                                                .addMeasurementItem(
                                                                        new EMeasurementDetailItemResponseModel()
                                                                                .unit("kwh")
                                                                                .offtakeValue(20.0)
                                                                                .injectionValue(10.0)
                                                                                .injectionValidationState("READ")
                                                                                .offtakeValidationState("READ")
                                                                )
                                                )
                                )
                );
        var id = new IntermediateValidatedHistoricalDocument(
                config,
                new IdentifiableMeteringData(pr, data),
                dataNeedsService
        );

        // When
        var vhds = id.toVHD();

        // Then
        var ignoredNames = Set.of(
                "messageDocumentHeader.creationDateTime",
                "createdDateTime",
                "dateAndOrTime.dateTime",
                "mRID"
        );
        var res = vhds.getFirst();
        var bytes = serde.serialize(res);
        var testXml = new String(bytes, StandardCharsets.UTF_8);
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(testXml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> ignoredNames.stream().noneMatch(node.getNodeName()::endsWith))
                                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
        assertTrue(XmlValidator.validateV104ValidatedHistoricalDataMarketDocument(bytes));
    }


    @Test
    void givenHourlyGasResponseData_whenToVhd_thenReturnVhd() throws SerializationException {
        // Given
        when(dataNeedsService.getById("dnid")).thenReturn(new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.NATURAL_GAS,
                Granularity.PT15M,
                Granularity.P1D
        ));
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <ns:VHD_Envelope xmlns:ns="https//eddie.energy/CIM/VHD_v1.04">
                    <ns:messageDocumentHeader.creationDateTime>ignored</ns:messageDocumentHeader.creationDateTime>
                    <ns:messageDocumentHeader.metaInformation.connectionId>cid</ns:messageDocumentHeader.metaInformation.connectionId>
                    <ns:messageDocumentHeader.metaInformation.dataNeedId>dnid</ns:messageDocumentHeader.metaInformation.dataNeedId>
                    <ns:messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</ns:messageDocumentHeader.metaInformation.documentType>
                    <ns:messageDocumentHeader.metaInformation.permissionId>pid</ns:messageDocumentHeader.metaInformation.permissionId>
                    <ns:messageDocumentHeader.metaInformation.region.connector>be-fluvius</ns:messageDocumentHeader.metaInformation.region.connector>
                    <ns:messageDocumentHeader.metaInformation.region.country>NBE</ns:messageDocumentHeader.metaInformation.region.country>
                    <ns:MarketDocument>
                        <ns:mRID>anything</ns:mRID>
                        <ns:revisionNumber>104</ns:revisionNumber>
                        <ns:description>1</ns:description>
                        <ns:type>A45</ns:type>
                        <ns:createdDateTime>ignored</ns:createdDateTime>
                        <ns:sender_MarketParticipant.marketRole.type>A26</ns:sender_MarketParticipant.marketRole.type>
                        <ns:receiver_MarketParticipant.marketRole.type>A13</ns:receiver_MarketParticipant.marketRole.type>
                        <ns:sender_MarketParticipant.mRID codingScheme="NBE">Fluvius</ns:sender_MarketParticipant.mRID>
                        <ns:receiver_MarketParticipant.mRID codingScheme="NBE">client-id</ns:receiver_MarketParticipant.mRID>
                        <ns:period.timeInterval>
                            <ns:start>2025-01-01T00:00Z</ns:start>
                            <ns:end>2025-01-01T01:00Z</ns:end>
                        </ns:period.timeInterval>
                        <ns:process.processType>A16</ns:process.processType>
                        <ns:TimeSeries>
                            <ns:mRID>ignored</ns:mRID>
                            <ns:version>2</ns:version>
                            <ns:businessType>A04</ns:businessType>
                            <ns:product>8716867000030</ns:product>
                            <ns:energy_Measurement_Unit.name>MTQ</ns:energy_Measurement_Unit.name>
                            <ns:flowDirection.direction>A02</ns:flowDirection.direction>
                            <ns:Period>
                                <ns:resolution>P0Y0M0DT1H0M0.000S</ns:resolution>
                                <ns:timeInterval>
                                    <ns:start>2025-01-01T00:00Z</ns:start>
                                    <ns:end>2025-01-01T01:00Z</ns:end>
                                </ns:timeInterval>
                                <ns:Point>
                                    <ns:position>1</ns:position>
                                    <ns:energy_Quantity.quantity>10.0</ns:energy_Quantity.quantity>
                                    <ns:energy_Quantity.quality>A04</ns:energy_Quantity.quality>
                                </ns:Point>
                            </ns:Period>
                            <ns:marketEvaluationPoint.meterReadings.readings.readingType.commodity>7</ns:marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                            <ns:marketEvaluationPoint.meterReadings.mRID codingScheme="NBE">mid</ns:marketEvaluationPoint.meterReadings.mRID>
                            <ns:reason.code>999</ns:reason.code>
                        </ns:TimeSeries>
                    </ns:MarketDocument>
                </ns:VHD_Envelope>
                """;
        var config = new FluviusOAuthConfiguration("token", "client-id", "secret", "tenant", "scope");
        var pr = DefaultFluviusPermissionRequestBuilder
                .create()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .granularity(Granularity.PT1H)
                .build();
        var timestampStart = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var timestampEnd = OffsetDateTime.of(2025, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);
        var data = new GetEnergyResponseModelApiDataResponse()
                .metaData(
                        new ApiMetaData()
                                .version("2")
                )
                .data(
                        new GetEnergyResponseModel()
                                .fetchTime(OffsetDateTime.now(ZoneOffset.UTC))
                                .addGasMetersItem(
                                        new GasMeterResponseModel()
                                                .seqNumber(1)
                                                .meterID("mid")
                                                .addHourlyEnergyItem(
                                                        new GHourlyEnergyItemResponseModel()
                                                                .timestampStart(timestampStart)
                                                                .timestampEnd(timestampEnd)
                                                                .addMeasurementItem(
                                                                        new GMeasurementDetailItemResponseModel()
                                                                                .unit("m3")
                                                                                .offtakeValue(10.0)
                                                                                .offtakeValidationState("READ")
                                                                )
                                                )
                                )
                );
        var id = new IntermediateValidatedHistoricalDocument(
                config,
                new IdentifiableMeteringData(pr, data),
                dataNeedsService
        );

        // When
        var vhds = id.toVHD();

        // Then
        var ignoredNames = Set.of(
                "messageDocumentHeader.creationDateTime",
                "createdDateTime",
                "dateAndOrTime.dateTime",
                "mRID"
        );
        var res = vhds.getFirst();
        var bytes = serde.serialize(res);
        var testXml = new String(bytes, StandardCharsets.UTF_8);
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(testXml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> ignoredNames.stream().noneMatch(node.getNodeName()::endsWith))
                                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
        assertTrue(XmlValidator.validateV104ValidatedHistoricalDataMarketDocument(bytes));
    }

    @Test
    void givenGasDailyResponseData_whenToVhd_thenReturnVhd() throws SerializationException {
        // Given
        when(dataNeedsService.getById("dnid")).thenReturn(new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.NATURAL_GAS,
                Granularity.PT15M,
                Granularity.P1D
        ));
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <ns:VHD_Envelope xmlns:ns="https//eddie.energy/CIM/VHD_v1.04">
                    <ns:messageDocumentHeader.creationDateTime>ignored</ns:messageDocumentHeader.creationDateTime>
                    <ns:messageDocumentHeader.metaInformation.connectionId>cid</ns:messageDocumentHeader.metaInformation.connectionId>
                    <ns:messageDocumentHeader.metaInformation.dataNeedId>dnid</ns:messageDocumentHeader.metaInformation.dataNeedId>
                    <ns:messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</ns:messageDocumentHeader.metaInformation.documentType>
                    <ns:messageDocumentHeader.metaInformation.permissionId>pid</ns:messageDocumentHeader.metaInformation.permissionId>
                    <ns:messageDocumentHeader.metaInformation.region.connector>be-fluvius</ns:messageDocumentHeader.metaInformation.region.connector>
                    <ns:messageDocumentHeader.metaInformation.region.country>NBE</ns:messageDocumentHeader.metaInformation.region.country>
                    <ns:MarketDocument>
                        <ns:mRID>anything</ns:mRID>
                        <ns:revisionNumber>104</ns:revisionNumber>
                        <ns:description>1</ns:description>
                        <ns:type>A45</ns:type>
                        <ns:createdDateTime>ignored</ns:createdDateTime>
                        <ns:sender_MarketParticipant.marketRole.type>A26</ns:sender_MarketParticipant.marketRole.type>
                        <ns:receiver_MarketParticipant.marketRole.type>A13</ns:receiver_MarketParticipant.marketRole.type>
                        <ns:sender_MarketParticipant.mRID codingScheme="NBE">Fluvius</ns:sender_MarketParticipant.mRID>
                        <ns:receiver_MarketParticipant.mRID codingScheme="NBE">client-id</ns:receiver_MarketParticipant.mRID>
                        <ns:period.timeInterval>
                            <ns:start>2025-01-01T00:00Z</ns:start>
                            <ns:end>2025-01-02T00:00Z</ns:end>
                        </ns:period.timeInterval>
                        <ns:process.processType>A16</ns:process.processType>
                        <ns:TimeSeries>
                            <ns:mRID>ignored</ns:mRID>
                            <ns:version>2</ns:version>
                            <ns:businessType>A04</ns:businessType>
                            <ns:product>8716867000030</ns:product>
                            <ns:energy_Measurement_Unit.name>MTQ</ns:energy_Measurement_Unit.name>
                            <ns:flowDirection.direction>A02</ns:flowDirection.direction>
                            <ns:Period>
                                <ns:resolution>P0Y0M1DT0H0M0.000S</ns:resolution>
                                <ns:timeInterval>
                                    <ns:start>2025-01-01T00:00Z</ns:start>
                                    <ns:end>2025-01-02T00:00Z</ns:end>
                                </ns:timeInterval>
                                <ns:Point>
                                    <ns:position>1</ns:position>
                                    <ns:energy_Quantity.quantity>10.0</ns:energy_Quantity.quantity>
                                    <ns:energy_Quantity.quality>A04</ns:energy_Quantity.quality>
                                </ns:Point>
                            </ns:Period>
                            <ns:marketEvaluationPoint.meterReadings.readings.readingType.commodity>7</ns:marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                            <ns:marketEvaluationPoint.meterReadings.mRID codingScheme="NBE">mid</ns:marketEvaluationPoint.meterReadings.mRID>
                            <ns:reason.code>999</ns:reason.code>
                        </ns:TimeSeries>
                    </ns:MarketDocument>
                </ns:VHD_Envelope>
                """;
        var config = new FluviusOAuthConfiguration("token", "client-id", "secret", "tenant", "scope");
        var pr = DefaultFluviusPermissionRequestBuilder
                .create()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .granularity(Granularity.P1D)
                .build();
        var timestampStart = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var timestampEnd = OffsetDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        var data = new GetEnergyResponseModelApiDataResponse()
                .metaData(
                        new ApiMetaData()
                                .version("2")
                )
                .data(
                        new GetEnergyResponseModel()
                                .fetchTime(OffsetDateTime.now(ZoneOffset.UTC))
                                .addGasMetersItem(
                                        new GasMeterResponseModel()
                                                .seqNumber(1)
                                                .meterID("mid")
                                                .addDailyEnergyItem(
                                                        new GDailyEnergyItemResponseModel()
                                                                .timestampStart(timestampStart)
                                                                .timestampEnd(timestampEnd)
                                                                .addMeasurementItem(
                                                                        new GMeasurementItemResponseModel()
                                                                                .unit("m3")
                                                                                .offtakeValue(10.0)
                                                                                .offtakeValidationState("READ")
                                                                )
                                                )
                                )
                );
        var id = new IntermediateValidatedHistoricalDocument(
                config,
                new IdentifiableMeteringData(pr, data),
                dataNeedsService
        );

        // When
        var vhds = id.toVHD();

        // Then
        var ignoredNames = Set.of(
                "messageDocumentHeader.creationDateTime",
                "createdDateTime",
                "dateAndOrTime.dateTime",
                "mRID"
        );
        var res = vhds.getFirst();
        var bytes = serde.serialize(res);
        var testXml = new String(bytes, StandardCharsets.UTF_8);
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(testXml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> ignoredNames.stream().noneMatch(node.getNodeName()::endsWith))
                                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
        assertTrue(XmlValidator.validateV104ValidatedHistoricalDataMarketDocument(bytes));
    }
}