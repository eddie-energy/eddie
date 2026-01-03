package energy.eddie.regionconnector.fr.enedis.providers.v1_04;

import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequestBuilder;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntermediateValidatedHistoricalDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();

    IntermediateValidatedHistoricalDocumentTest() throws SerdeInitializationException {}

    @Test
    void givenIdentifiableMeterReading_whenValue_thenReturnValidatedHistoricalDataMarketDocument() throws IOException, SerializationException {
        // Given
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <VHD_Envelope xmlns="https//eddie.energy/CIM/VHD_v1.04">
                  <messageDocumentHeader.creationDateTime>2025-11-06T11:52:45Z</messageDocumentHeader.creationDateTime>
                  <messageDocumentHeader.metaInformation.connectionId>cid</messageDocumentHeader.metaInformation.connectionId>
                  <messageDocumentHeader.metaInformation.dataNeedId>dnid</messageDocumentHeader.metaInformation.dataNeedId>
                  <messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</messageDocumentHeader.metaInformation.documentType>
                  <messageDocumentHeader.metaInformation.permissionId>pid</messageDocumentHeader.metaInformation.permissionId>
                  <messageDocumentHeader.metaInformation.region.connector>fr-enedis</messageDocumentHeader.metaInformation.region.connector>
                  <messageDocumentHeader.metaInformation.region.country>NFR</messageDocumentHeader.metaInformation.region.country>
                  <MarketDocument>
                    <mRID>uuid</mRID>
                    <revisionNumber>104</revisionNumber>
                    <type>A45</type>
                    <createdDateTime>2024-02-15T10:10:07Z</createdDateTime>
                    <sender_MarketParticipant.mRID codingScheme="NFR">Enedis</sender_MarketParticipant.mRID>
                    <sender_MarketParticipant.marketRole.type>A26</sender_MarketParticipant.marketRole.type>
                    <receiver_MarketParticipant.mRID codingScheme="NFR">client-id</receiver_MarketParticipant.mRID>
                    <receiver_MarketParticipant.marketRole.type>A13</receiver_MarketParticipant.marketRole.type>
                    <period.timeInterval>
                      <start>2024-01-31T23:00Z</start>
                      <end>2024-02-07T23:00Z</end>
                    </period.timeInterval>
                    <process.processType>A16</process.processType>
                    <TimeSeries>
                      <version>1</version>
                      <mRID>uuid</mRID>
                      <businessType>A04</businessType>
                      <product>8716867000030</product>
                      <energy_Measurement_Unit.name>KWH</energy_Measurement_Unit.name>
                      <flowDirection.direction>A02</flowDirection.direction>
                      <Period>
                        <resolution>P0Y0M1DT0H0M0.000S</resolution>
                        <timeInterval>
                          <start>2024-01-31T23:00Z</start>
                          <end>2024-02-07T23:00Z</end>
                        </timeInterval>
                        <Point>
                          <position>1</position>
                          <energy_Quantity.quantity>0.0</energy_Quantity.quantity>
                          <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                        <Point>
                          <position>2</position>
                          <energy_Quantity.quantity>0.0</energy_Quantity.quantity>
                          <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                        <Point>
                          <position>3</position>
                          <energy_Quantity.quantity>0.0</energy_Quantity.quantity>
                          <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                        <Point>
                          <position>4</position>
                          <energy_Quantity.quantity>0.0</energy_Quantity.quantity>
                          <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                        <Point>
                          <position>5</position>
                          <energy_Quantity.quantity>0.0</energy_Quantity.quantity>
                          <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                        <Point>
                          <position>6</position>
                          <energy_Quantity.quantity>0.0</energy_Quantity.quantity>
                          <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                        <Point>
                          <position>7</position>
                          <energy_Quantity.quantity>0.0</energy_Quantity.quantity>
                          <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                      </Period>
                      <marketEvaluationPoint.mRID codingScheme="NFR">24115050XXXXXX</marketEvaluationPoint.mRID>
                      <marketEvaluationPoint.meterReadings.readings.readingType.aggregate>26</marketEvaluationPoint.meterReadings.readings.readingType.aggregate>
                      <marketEvaluationPoint.meterReadings.readings.readingType.commodity>2</marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                      <reason.code>999</reason.code>
                    </TimeSeries>
                  </MarketDocument>
                </VHD_Envelope>
                """;
        var response = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK);
        var pr = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .create();
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                new IdentifiableMeterReading(pr, response, MeterReadingType.CONSUMPTION),
                new EnedisConfiguration("client-id", "secret", "https://localhost")
        );

        // When
        var res = intermediateVHD.value();

        // Then
        var bytes = serde.serialize(res);
        var testXml = new String(bytes, StandardCharsets.UTF_8);
        System.out.println(testXml);
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
                "mRID",
                "createdDateTime",
                "messageDocumentHeader.creationDateTime",
                "dateAndOrTime.dateTime"
        );
    }
}