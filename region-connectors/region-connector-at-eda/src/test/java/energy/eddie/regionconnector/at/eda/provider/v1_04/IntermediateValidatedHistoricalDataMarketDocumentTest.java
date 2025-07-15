package energy.eddie.regionconnector.at.eda.provider.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.StandardDirectionTypeList;
import energy.eddie.cim.v1_04.StandardQualityTypeList;
import energy.eddie.outbound.shared.serde.SerdeInitializationException;
import energy.eddie.outbound.shared.serde.SerializationException;
import energy.eddie.outbound.shared.serde.XmlMessageSerde;
import energy.eddie.outbound.shared.testing.XmlValidator;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.builder.DiffBuilder;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntermediateValidatedHistoricalDataMarketDocumentTest {

    private final XmlMessageSerde serde = new XmlMessageSerde();

    IntermediateValidatedHistoricalDataMarketDocumentTest() throws SerdeInitializationException {}

    @ParameterizedTest
    @MethodSource("meterCodeAndMeteringModeSource")
    // CIM mapping requires that amount of asserts
    @SuppressWarnings("java:S5961")
    void toVhd_returnsVHDEnvelopes(
            String meterCode,
            StandardDirectionTypeList expectedDirection,
            String meteringMode,
            StandardQualityTypeList expectedQuality
    ) throws SerializationException {
        // Given
        var ignoredNames = Set.of(
                "ns5:messageDocumentHeader.creationDateTime",
                "ns5:createdDateTime",
                "ns5:dateAndOrTime.dateTime"
        );
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
                	<ns5:messageDocumentHeader.metaInformation.region.connector>at-eda</ns5:messageDocumentHeader.metaInformation.region.connector>
                	<ns5:messageDocumentHeader.metaInformation.region.country>AT</ns5:messageDocumentHeader.metaInformation.region.country>
                	<ns5:MarketDocument>
                		<ns5:mRID>messageId</ns5:mRID>
                		<ns5:createdDateTime>2024-12-31T23:00:00Z</ns5:createdDateTime>
                		<ns5:sender_MarketParticipant.mRID codingScheme="NAT">eda</ns5:sender_MarketParticipant.mRID>
                		<ns5:sender_MarketParticipant.marketRole.type>A26</ns5:sender_MarketParticipant.marketRole.type>
                		<ns5:receiver_MarketParticipant.mRID codingScheme="NAT">eddie</ns5:receiver_MarketParticipant.mRID>
                		<ns5:receiver_MarketParticipant.marketRole.type>A13</ns5:receiver_MarketParticipant.marketRole.type>
                		<ns5:period.timeInterval>
                			<ns5:start>2024-12-31T23:00Z</ns5:start>
                			<ns5:end>2025-01-01T23:00Z</ns5:end>
                		</ns5:period.timeInterval>
                		<ns5:process.processType>A16</ns5:process.processType>
                		<ns5:TimeSeries>
                			<ns5:version>1</ns5:version>
                			<ns5:dateAndOrTime.dateTime>2024-12-31T23:00:00Z</ns5:dateAndOrTime.dateTime>
                			<ns5:energy_Measurement_Unit.name>KWH</ns5:energy_Measurement_Unit.name>
                			<ns5:flowDirection.direction>%s</ns5:flowDirection.direction>
                			<ns5:Period>
                				<ns5:resolution>P0Y0M1DT0H0M0.000S</ns5:resolution>
                				<ns5:timeInterval>
                					<ns5:start>2024-12-31T23:00Z</ns5:start>
                					<ns5:end>2025-01-02T22:59Z</ns5:end>
                				</ns5:timeInterval>
                				<ns5:Point>
                					<ns5:position>1</ns5:position>
                					<ns5:energy_Quantity.quantity>1</ns5:energy_Quantity.quantity>
                					<ns5:energy_Quantity.quality>%s</ns5:energy_Quantity.quality>
                				</ns5:Point>
                			</ns5:Period>
                			<ns5:registeredResource.mRID codingScheme="NAT">%s</ns5:registeredResource.mRID>
                			<ns5:marketEvaluationPoint.mRID codingScheme="NAT">%s</ns5:marketEvaluationPoint.mRID>
                			<ns5:reason.code>999</ns5:reason.code>
                			<ns5:reason.text>reason</ns5:reason.text>
                			<ns5:energyQuality_Measurement_Unit.name>C62</ns5:energyQuality_Measurement_Unit.name>
                		</ns5:TimeSeries>
                	</ns5:MarketDocument>
                </ns5:VHD_Envelope>
                """.formatted(expectedDirection.value(), expectedQuality.value(), meterCode, meterCode);
        var pr = new SimplePermissionRequest("pid", "cid", "dnid");
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);
        var now = ZonedDateTime.now(AT_ZONE_ID);
        var cal = DatatypeFactory.newDefaultInstance()
                                 .newXMLGregorianCalendar(GregorianCalendar.from(now));
        var simpleRecord = new SimpleEdaConsumptionRecord()
                .setMessageId("messageId")
                .setConversationId("conversationId")
                .setMeteringPoint(meterCode)
                .setStartDate(start)
                .setEndDate(end)
                .setSenderMessageAddress("eda")
                .setReceiverMessageAddress("eddie")
                .setDocumentCreationDateTime(now)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(Granularity.P1D)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setEnergyPositions(List.of(
                                                        new EnergyPosition(BigDecimal.ONE, meteringMode)
                                                ))
                                                .setMeterCode(meterCode)
                                                .setBillingUnit("KWH")
                                ))
                                .setMeterReadingStart(start.atStartOfDay(AT_ZONE_ID))
                                .setMeterReadingEnd(endOfDay(end, AT_ZONE_ID))
                                .setMeteringReason("reason")
                ))
                .setSchemaVersion("version")
                .setProcessDate(cal);
        var consumptionRecord = new IdentifiableConsumptionRecord(simpleRecord, List.of(pr), start, end);
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "epID");
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord);

        // When
        var res = doc.toVhd();

        // Then
        var testXml = new String(serde.serialize(res.getFirst()), StandardCharsets.UTF_8);
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(testXml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> !ignoredNames.contains(node.getNodeName()))
                                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
    }

    @Test
    void toVhd_producesValidXml() throws SerializationException {
        // Given
        var pr = new SimplePermissionRequest("pid", "cid", "dnid");
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);
        var created = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, AT_ZONE_ID);
        var cal = DatatypeFactory.newDefaultInstance()
                                 .newXMLGregorianCalendar(GregorianCalendar.from(created));
        var simpleRecord = new SimpleEdaConsumptionRecord()
                .setMessageId("messageId")
                .setConversationId("conversationId")
                .setMeteringPoint("1-1:1.9.0 P.01")
                .setStartDate(start)
                .setEndDate(end)
                .setSenderMessageAddress("eda")
                .setReceiverMessageAddress("eddie")
                .setDocumentCreationDateTime(created)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(Granularity.P1D)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setEnergyPositions(List.of(
                                                        new EnergyPosition(BigDecimal.ONE, "L1")
                                                ))
                                                .setMeterCode("1-1:1.9.0 P.01")
                                                .setBillingUnit("KWH")
                                ))
                                .setMeterReadingStart(start.atStartOfDay(AT_ZONE_ID))
                                .setMeterReadingEnd(endOfDay(end, AT_ZONE_ID))
                                .setMeteringReason("reason")
                ))
                .setSchemaVersion("version")
                .setProcessDate(cal);
        var consumptionRecord = new IdentifiableConsumptionRecord(simpleRecord, List.of(pr), start, end);
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "epID");
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord);

        // When
        var res = doc.toVhd();

        // Then
        var xmlDoc = serde.serialize(res.getFirst());
        var xmlStr = new String(xmlDoc, StandardCharsets.UTF_8);
        assertTrue(XmlValidator.validateV104ValidatedHistoricalDataMarketDocument(xmlDoc),
                   "Failed to validate XML, see:\n" + xmlStr);
    }

    private static Stream<Arguments> meterCodeAndMeteringModeSource() {
        return Stream.of(
                Arguments.of("1-1:1.9.0 P.01",
                             StandardDirectionTypeList.DOWN,
                             "L1",
                             StandardQualityTypeList.AS_PROVIDED),
                Arguments.of("1-1:2.9.0 P.01", StandardDirectionTypeList.UP, "L2", StandardQualityTypeList.ADJUSTED),
                Arguments.of("1-1:2.9.0 P.01", StandardDirectionTypeList.UP, "L3", StandardQualityTypeList.ESTIMATED)
        );
    }
}