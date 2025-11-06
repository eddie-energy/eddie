package energy.eddie.regionconnector.simulation.providers.cim.v1_04;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();

    IntermediateValidatedHistoricalDataMarketDocumentTest() throws SerdeInitializationException {}

    @Test
    void givenSimulatedMeterReading_whenValue_returnsValidVHDEnvelope() throws SerializationException {
        // Given
        //language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <VHD_Envelope xmlns="https//eddie.energy/CIM/VHD_v1.04"    >
                  <messageDocumentHeader.creationDateTime>2025-11-06T11:52:45Z</messageDocumentHeader.creationDateTime>
                  <messageDocumentHeader.metaInformation.connectionId>cid</messageDocumentHeader.metaInformation.connectionId>
                  <messageDocumentHeader.metaInformation.dataNeedId>dnid</messageDocumentHeader.metaInformation.dataNeedId>
                  <messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</messageDocumentHeader.metaInformation.documentType>
                  <messageDocumentHeader.metaInformation.permissionId>pid</messageDocumentHeader.metaInformation.permissionId>
                  <messageDocumentHeader.metaInformation.region.connector>sim</messageDocumentHeader.metaInformation.region.connector>
                  <messageDocumentHeader.metaInformation.region.country>NDE</messageDocumentHeader.metaInformation.region.country>
                  <MarketDocument>
                    <mRID>1023ff30-35f5-4c8c-ac4a-32356278bab2</mRID>
                    <revisionNumber>104</revisionNumber>
                    <type>A45</type>
                    <createdDateTime>2025-11-06T11:52:45Z</createdDateTime>
                    <sender_MarketParticipant.mRID codingScheme="A01">sim</sender_MarketParticipant.mRID>
                    <sender_MarketParticipant.marketRole.type>A26</sender_MarketParticipant.marketRole.type>
                    <receiver_MarketParticipant.mRID codingScheme="NAT">sim</receiver_MarketParticipant.mRID>
                    <receiver_MarketParticipant.marketRole.type>A13</receiver_MarketParticipant.marketRole.type>
                    <period.timeInterval>
                      <start>2025-11-06T00:00Z</start>
                      <end>2025-11-06T00:15Z</end>
                    </period.timeInterval>
                    <process.processType>A16</process.processType>
                    <TimeSeries>
                      <version>1</version>
                      <mRID>1e921378-3f09-49ed-a2fc-3343b8b9548f</mRID>
                      <businessType>A04</businessType>
                      <product>8716867000030</product>
                      <energy_Measurement_Unit.name>WTT</energy_Measurement_Unit.name>
                      <flowDirection.direction>A02</flowDirection.direction>
                      <Period>
                        <resolution>P0Y0M0DT0H15M0.000S</resolution>
                        <timeInterval>
                            <start>2025-11-06T00:00Z</start>
                            <end>2025-11-06T00:15Z</end>
                        </timeInterval>
                        <Point>
                          <position>1</position>
                          <energy_Quantity.quantity>10.0</energy_Quantity.quantity>
                          <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                        <reason.code>999</reason.code>
                      </Period>
                      <marketEvaluationPoint.mRID codingScheme="NFR">mid</marketEvaluationPoint.mRID>
                      <marketEvaluationPoint.meterReadings.readings.readingType.aggregate>26</marketEvaluationPoint.meterReadings.readings.readingType.aggregate>
                      <marketEvaluationPoint.meterReadings.readings.readingType.commodity>0</marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                      <reason.code>999</reason.code>
                    </TimeSeries>
                  </MarketDocument>
                </VHD_Envelope>
                """;
        var now = ZonedDateTime.of(2025, 11, 6, 0, 0, 0, 0, ZoneOffset.UTC);
        var cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "EP-ID");
        var simulatedMeterReading = new SimulatedMeterReading("cid",
                                                              "dnid",
                                                              "pid",
                                                              "mid",
                                                              now,
                                                              PT15M.toString(),
                                                              List.of(new Measurement(
                                                                      10.0,
                                                                      Measurement.MeasurementType.MEASURED
                                                              ))
        );
        var intermediate = new IntermediateValidatedHistoricalDataMarketDocument(simulatedMeterReading, cimConfig);

        // When
        var res = intermediate.value();

        // Then
        var bytes = serde.serialize(res);
        var testXml = new String(bytes, StandardCharsets.UTF_8);
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(testXml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> ignoredNames().stream().noneMatch(node.getNodeName()::endsWith))
                                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
        assertTrue(XmlValidator.validateV104ValidatedHistoricalDataMarketDocument(bytes));
    }

    private static Set<String> ignoredNames() {
        return Set.of(
                "messageDocumentHeader.creationDateTime",
                "createdDateTime",
                "dateAndOrTime.dateTime",
                "mRID"
        );
    }
}