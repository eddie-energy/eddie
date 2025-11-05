package energy.eddie.regionconnector.cds.providers.vhd.v1_04;

import energy.eddie.cim.v1_04.StandardBusinessTypeList;
import energy.eddie.cim.v1_04.StandardDirectionTypeList;
import energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.outbound.shared.serde.SerdeInitializationException;
import energy.eddie.outbound.shared.serde.SerializationException;
import energy.eddie.outbound.shared.serde.XmlMessageSerde;
import energy.eddie.outbound.shared.testing.XmlValidator;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.cim.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.builder.DiffBuilder;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();

    IntermediateValidatedHistoricalDataMarketDocumentTest() throws SerdeInitializationException {}

    @ParameterizedTest
    @MethodSource
    void testToVhds_returnsVhds(
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum format,
            StandardBusinessTypeList businessType,
            StandardDirectionTypeList flowDirection,
            CommodityKind commodity,
            StandardUnitOfMeasureTypeList unit,
            BigDecimal value
    ) throws SerializationException {
        // Given
        var businessTypeStr = businessType == null ? null : businessType.value();
        var unitStr = unit == null ? null : unit.value();
        var commodityStr = commodity == null ? null : commodity.value();
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <ns5:VHD_Envelope xmlns="http://www.eddie.energy/Consent/EDD02/20240125"
                	xmlns:ns5="https//eddie.energy/CIM/VHD_v1.04">
                	<ns5:messageDocumentHeader.creationDateTime>2025-07-15T10:36:40Z</ns5:messageDocumentHeader.creationDateTime>
                	<ns5:messageDocumentHeader.metaInformation.connectionId>cid</ns5:messageDocumentHeader.metaInformation.connectionId>
                	<ns5:messageDocumentHeader.metaInformation.dataNeedId>dnid</ns5:messageDocumentHeader.metaInformation.dataNeedId>
                	<ns5:messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</ns5:messageDocumentHeader.metaInformation.documentType>
                	<ns5:messageDocumentHeader.metaInformation.permissionId>pid</ns5:messageDocumentHeader.metaInformation.permissionId>
                	<ns5:messageDocumentHeader.metaInformation.region.connector>cds</ns5:messageDocumentHeader.metaInformation.region.connector>
                	<ns5:messageDocumentHeader.metaInformation.region.country>us</ns5:messageDocumentHeader.metaInformation.region.country>
                	<ns5:MarketDocument>
                		<ns5:mRID>any</ns5:mRID>
                		<ns5:revisionNumber>104</ns5:revisionNumber>
                		<ns5:createdDateTime>2024-12-31T23:00:00Z</ns5:createdDateTime>
                		<ns5:type>A45</ns5:type>
                		<ns5:sender_MarketParticipant.mRID codingScheme="NUS">CDSC</ns5:sender_MarketParticipant.mRID>
                		<ns5:sender_MarketParticipant.marketRole.type>A26</ns5:sender_MarketParticipant.marketRole.type>
                		<ns5:receiver_MarketParticipant.mRID codingScheme="NUS">customer-number</ns5:receiver_MarketParticipant.mRID>
                		<ns5:receiver_MarketParticipant.marketRole.type>A13</ns5:receiver_MarketParticipant.marketRole.type>
                		<ns5:period.timeInterval>
                			<ns5:start>2025-01-01T00:00Z</ns5:start>
                			<ns5:end>2025-01-01T00:00Z</ns5:end>
                		</ns5:period.timeInterval>
                		<ns5:process.processType>A16</ns5:process.processType>
                		<ns5:TimeSeries>
                            <ns5:version>1</ns5:version>
                			<ns5:businessType>%s</ns5:businessType>
                			<ns5:dateAndOrTime.dateTime>2024-12-31T23:00:00Z</ns5:dateAndOrTime.dateTime>
                			<ns5:product>8716867000016</ns5:product>
                			<ns5:energy_Measurement_Unit.name>%s</ns5:energy_Measurement_Unit.name>
                			<ns5:flowDirection.direction>%s</ns5:flowDirection.direction>
                			<ns5:Period>
                				<ns5:resolution>P0Y0M0DT0H15M0.000S</ns5:resolution>
                				<ns5:timeInterval>
                					<ns5:start>2025-01-01T00:00Z</ns5:start>
                					<ns5:end>2025-01-01T00:00Z</ns5:end>
                				</ns5:timeInterval>
                				<ns5:Point>
                					<ns5:position>1</ns5:position>
                					<ns5:energy_Quantity.quantity>%s</ns5:energy_Quantity.quantity>
                					<ns5:energy_Quantity.quality>A04</ns5:energy_Quantity.quality>
                				</ns5:Point>
                			</ns5:Period>
                			<ns5:marketEvaluationPoint.mRID codingScheme="NUS">meter-number</ns5:marketEvaluationPoint.mRID>
                			<ns5:marketEvaluationPoint.meterReadings.readings.readingType.aggregate>26</ns5:marketEvaluationPoint.meterReadings.readings.readingType.aggregate>
                			<!--suppress XmlUnresolvedReference -->
                            <ns5:marketEvaluationPoint.meterReadings.readings.readingType.commodity>%s</ns5:marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                			<ns5:reason.code>999</ns5:reason.code>
                		</ns5:TimeSeries>
                	</ns5:MarketDocument>
                </ns5:VHD_Envelope>
                """.formatted(businessTypeStr, unitStr, flowDirection.value(), value, commodityStr);
        var now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var acc = createAccount(900, format, now);
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .setDataStart(now.toLocalDate())
                .setDataEnd(now.toLocalDate())
                .build();
        var intermediateDocs = new IntermediateValidatedHistoricalDataMarketDocument(pr, List.of(acc, acc));

        // When
        var res = intermediateDocs.toVhds();

        // Then
        var ignoredNames = ignoredNames(businessType, commodity, unit);
        var bytes = serde.serialize(res.getFirst());
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
    void testToVhds_withUnknownGranularity_returnsVhds() {
        // Given
        var now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var acc = createAccount(1, UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_FWD, now);
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .setDataStart(now.toLocalDate())
                .setDataEnd(now.toLocalDate())
                .build();
        var intermediateDocs = new IntermediateValidatedHistoricalDataMarketDocument(pr, List.of(acc));
        var expected = DatatypeFactory.newDefaultInstance().newDuration(Duration.ofSeconds(1).toMillis());

        // When
        var res = intermediateDocs.toVhds();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(VHDEnvelope::getMarketDocument)
                .extracting(VHDMarketDocument::getTimeSeries)
                .asInstanceOf(InstanceOfAssertFactories.list(TimeSeries.class))
                .singleElement()
                .extracting(TimeSeries::getPeriods)
                .asInstanceOf(InstanceOfAssertFactories.list(SeriesPeriod.class))
                .singleElement()
                .extracting(SeriesPeriod::getResolution)
                .isEqualTo(expected);
    }

    @Test
    void testToVhds_removesDataOutsideOfStartAndEnd() {
        // Given
        var now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var acc = createAccount(900, UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.DEMAND_KW, now);
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .setDataStart(now.plusDays(1).toLocalDate())
                .setDataEnd(now.plusDays(2).toLocalDate())
                .build();
        var intermediateDocs = new IntermediateValidatedHistoricalDataMarketDocument(pr, List.of(acc, acc));

        // When
        var res = intermediateDocs.toVhds();

        // Then
        assertThat(res)
                .hasSize(2)
                .first()
                .extracting(VHDEnvelope::getMarketDocument)
                .extracting(VHDMarketDocument::getTimeSeries)
                .asInstanceOf(InstanceOfAssertFactories.list(TimeSeries.class))
                .singleElement()
                .extracting(TimeSeries::getPeriods)
                .asInstanceOf(InstanceOfAssertFactories.list(SeriesPeriod.class))
                .singleElement()
                .extracting(SeriesPeriod::getPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(Point.class))
                .isEmpty();
    }

    private static HashSet<String> ignoredNames(
            StandardBusinessTypeList businessType,
            CommodityKind commodity,
            StandardUnitOfMeasureTypeList unit
    ) {
        var ignoredNames = new HashSet<>(
                Set.of(
                        "messageDocumentHeader.creationDateTime",
                        "createdDateTime",
                        "dateAndOrTime.dateTime",
                        "mRID"
                ));
        if (businessType == null) {
            ignoredNames.add("businessType");
        }
        if (unit == null) {
            ignoredNames.add("energy_Measurement_Unit.name");
        }
        if (commodity == null) {
            ignoredNames.add("marketEvaluationPoint.meterReadings.readings.readingType.commodity");
        }
        return ignoredNames;
    }

    @SuppressWarnings("unused") // errorprone  false positive
    private static Stream<Arguments> testToVhds_returnsVhds() {
        return Stream.of(
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_FWD,
                             StandardBusinessTypeList.CONSUMPTION,
                             StandardDirectionTypeList.DOWN,
                             CommodityKind.ELECTRICITYPRIMARYMETERED,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
                             BigDecimal.ONE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_REV,
                             StandardBusinessTypeList.PRODUCTION,
                             StandardDirectionTypeList.UP,
                             CommodityKind.ELECTRICITYPRIMARYMETERED,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
                             BigDecimal.ONE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_NET,
                             null,
                             StandardDirectionTypeList.UP_AND_DOWN,
                             CommodityKind.ELECTRICITYPRIMARYMETERED,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
                             BigDecimal.ONE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.WATER_GAL,
                             StandardBusinessTypeList.CONSUMPTION,
                             StandardDirectionTypeList.DOWN,
                             CommodityKind.POTABLEWATER,
                             StandardUnitOfMeasureTypeList.CUBIC_METRE,
                             CimUnitConverter.GAL_TO_CUBIC_METRE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_CCF,
                             StandardBusinessTypeList.CONSUMPTION,
                             StandardDirectionTypeList.DOWN,
                             CommodityKind.NATURALGAS,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
                             CimUnitConverter.CCF_TO_KWH),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.EACS,
                             StandardBusinessTypeList.CONSUMPTION,
                             StandardDirectionTypeList.DOWN,
                             CommodityKind.NONE,
                             StandardUnitOfMeasureTypeList.ONE,
                             BigDecimal.ONE)
        );
    }

    private static Account createAccount(
            int interval,
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum format,
            ZonedDateTime timestamp
    ) {
        var usageSegment = new UsageSegment(
                timestamp,
                timestamp,
                BigDecimal.valueOf(interval),
                Map.of(format, List.of(BigDecimal.ONE))
        );
        var meter = new Meter("meter-number", "cds-id", List.of(usageSegment));
        return new Account("customer-number",
                           "customer name",
                           "business",
                           List.of(new ServiceContract(
                                   "",
                                   "",
                                   List.of(new ServicePoint("", List.of(meter)))
                           ))
        );
    }
}