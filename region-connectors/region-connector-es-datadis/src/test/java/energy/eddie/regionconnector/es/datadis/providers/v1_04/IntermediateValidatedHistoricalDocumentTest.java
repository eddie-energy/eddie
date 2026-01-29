// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
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
    void givenMeterReadings_whenValue_thenReturnValidatedHistoricalDataMarketDocument() throws SerializationException, IOException {
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
                  <messageDocumentHeader.metaInformation.region.connector>es-datadis</messageDocumentHeader.metaInformation.region.connector>
                  <messageDocumentHeader.metaInformation.region.country>NES</messageDocumentHeader.metaInformation.region.country>
                  <MarketDocument>
                    <mRID>uuid</mRID>
                    <revisionNumber>104</revisionNumber>
                    <type>A45</type>
                    <createdDateTime>2024-02-15T10:10:07Z</createdDateTime>
                    <sender_MarketParticipant.mRID codingScheme="NES">Datadis</sender_MarketParticipant.mRID>
                    <sender_MarketParticipant.marketRole.type>A26</sender_MarketParticipant.marketRole.type>
                    <receiver_MarketParticipant.mRID codingScheme="NES">client-id</receiver_MarketParticipant.mRID>
                    <receiver_MarketParticipant.marketRole.type>A13</receiver_MarketParticipant.marketRole.type>
                    <period.timeInterval>
                      <start>2023-11-30T23:00Z</start>
                      <end>2023-11-30T23:00Z</end>
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
                        <resolution>P0Y0M0DT1H0M0.000S</resolution>
                        <timeInterval>
                          <start>2023-11-30T23:00Z</start>
                          <end>2023-11-30T23:00Z</end>
                        </timeInterval>
                        <Point>
                          <position>1</position>
                          <energy_Quantity.quantity>0.979</energy_Quantity.quantity>
                          <energy_Quantity.quality>A02</energy_Quantity.quality>
                        </Point>
                        <Point>
                          <position>2</position>
                          <energy_Quantity.quantity>0.939</energy_Quantity.quantity>
                          <energy_Quantity.quality>A02</energy_Quantity.quality>
                        </Point>
                        <Point>
                          <position>3</position>
                          <energy_Quantity.quantity>0.982</energy_Quantity.quantity>
                          <energy_Quantity.quality>A02</energy_Quantity.quality>
                        </Point>
                      </Period>
                      <registeredResource.mRID codingScheme="NES">ES00210000XXXXXXXXXX</registeredResource.mRID>
                      <marketEvaluationPoint.mRID codingScheme="NES">mid</marketEvaluationPoint.mRID>
                      <marketEvaluationPoint.meterReadings.readings.readingType.aggregate>26</marketEvaluationPoint.meterReadings.readings.readingType.aggregate>
                      <marketEvaluationPoint.meterReadings.readings.readingType.commodity>2</marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                      <reason.code>999</reason.code>
                    </TimeSeries>
                  </MarketDocument>
                </VHD_Envelope>
                """;
        var response = MeteringDataProvider.loadMeteringDataShort();
        var pr = new DatadisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setMeteringPointId("mid")
                .setGranularity(Granularity.PT1H)
                .build();
        var intermediateMeteringDataMono = IntermediateMeteringData.fromMeteringData(response).block();
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                new IdentifiableMeteringData(pr, intermediateMeteringDataMono),
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "EP-ID"),
                new DatadisConfiguration("client-id", "secret", "https://localhost")
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