// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04;

import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();

    IntermediateValidatedHistoricalDataMarketDocumentTest() throws SerdeInitializationException {}

    @Test
    void toVhd_withValues_returnsVhdsWithTimeSeries() throws SerializationException {
        // Given
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <VHD_Envelope xmlns="https//eddie.energy/CIM/VHD_v1.04"    >
                  <messageDocumentHeader.creationDateTime>2025-11-06T11:52:45Z</messageDocumentHeader.creationDateTime>
                  <messageDocumentHeader.metaInformation.connectionId>cid</messageDocumentHeader.metaInformation.connectionId>
                  <messageDocumentHeader.metaInformation.dataNeedId>dnid</messageDocumentHeader.metaInformation.dataNeedId>
                  <messageDocumentHeader.metaInformation.documentType>validated-historical-data-market-document</messageDocumentHeader.metaInformation.documentType>
                  <messageDocumentHeader.metaInformation.permissionId>pid</messageDocumentHeader.metaInformation.permissionId>
                  <messageDocumentHeader.metaInformation.region.connector>fi-fingrid</messageDocumentHeader.metaInformation.region.connector>
                  <messageDocumentHeader.metaInformation.region.country>NFI</messageDocumentHeader.metaInformation.region.country>
                  <MarketDocument>
                    <mRID>1023ff30-35f5-4c8c-ac4a-32356278bab2</mRID>
                    <revisionNumber>104</revisionNumber>
                    <type>A45</type>
                    <createdDateTime>2025-11-06T11:52:45Z</createdDateTime>
                    <sender_MarketParticipant.mRID codingScheme="NFI">1111111111111</sender_MarketParticipant.mRID>
                    <sender_MarketParticipant.marketRole.type>A26</sender_MarketParticipant.marketRole.type>
                    <receiver_MarketParticipant.mRID codingScheme="NFI">0000000000000</receiver_MarketParticipant.mRID>
                    <receiver_MarketParticipant.marketRole.type>A13</receiver_MarketParticipant.marketRole.type>
                    <period.timeInterval>
                      <start>2024-07-21T00:00Z</start>
                      <end>2024-07-30T00:00Z</end>
                    </period.timeInterval>
                    <process.processType>A16</process.processType>
                    <TimeSeries>
                      <version>1</version>
                      <mRID>1e921378-3f09-49ed-a2fc-3343b8b9548f</mRID>
                      <product>8716867000030</product>
                      <energy_Measurement_Unit.name>KWH</energy_Measurement_Unit.name>
                      <flowDirection.direction>A03</flowDirection.direction>
                      <Period>
                        <resolution>P0Y0M0DT1H0M0.000S</resolution>
                        <timeInterval>
                          <start>2024-07-21T00:00Z</start>
                          <end>2024-07-30T00:00Z</end>
                        </timeInterval>
                        <Point>
                          <position>1</position>
                          <energyQuality_Quantity.quantity>1.509000</energyQuality_Quantity.quantity>
                          <energyQuality_Quantity.quality>A04</energyQuality_Quantity.quality>
                        </Point>
                        <Point>
                            <position>2</position>
                            <energyQuality_Quantity.quantity>3.353000</energyQuality_Quantity.quantity>
                            <energyQuality_Quantity.quality>A04</energyQuality_Quantity.quality>
                        </Point>
                      </Period>
                      <marketEvaluationPoint.mRID codingScheme="NFI">642502030419633983</marketEvaluationPoint.mRID>
                      <marketEvaluationPoint.meterReadings.readings.readingType.commodity>2</marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                      <reason.code>999</reason.code>
                    </TimeSeries>
                  </MarketDocument>
                </VHD_Envelope>
                """;
        var response = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .build();
        var intermediateVHD = new IntermediateValidatedHistoricalDataMarketDocument(List.of(response), pr);

        // When
        var res = intermediateVHD.toVhds().getFirst();

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
        System.out.println(testXml);
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