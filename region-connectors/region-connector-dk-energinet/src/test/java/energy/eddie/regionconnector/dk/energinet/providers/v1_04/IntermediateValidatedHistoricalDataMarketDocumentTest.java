// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.DtoLoader;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();

    IntermediateValidatedHistoricalDataMarketDocumentTest() throws SerdeInitializationException {}

    @Test
    void toVhd_withValues_returnsVhdsWithTimeSeries() throws SerializationException, IOException {
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
                  <messageDocumentHeader.metaInformation.region.connector>dk-energinet</messageDocumentHeader.metaInformation.region.connector>
                  <messageDocumentHeader.metaInformation.region.country>NDK</messageDocumentHeader.metaInformation.region.country>
                  <MarketDocument>
                    <mRID>80024a18-0001-ef00-b63f-84710c7967XX</mRID>
                    <revisionNumber>104</revisionNumber>
                    <type>A45</type>
                    <createdDateTime>2024-02-15T10:10:07Z</createdDateTime>
                    <sender_MarketParticipant.mRID codingScheme="A10">5790001330583</sender_MarketParticipant.mRID>
                    <sender_MarketParticipant.marketRole.type>A26</sender_MarketParticipant.marketRole.type>
                    <receiver_MarketParticipant.mRID codingScheme="NAT">ep-id</receiver_MarketParticipant.mRID>
                    <receiver_MarketParticipant.marketRole.type>A13</receiver_MarketParticipant.marketRole.type>
                    <period.timeInterval>
                      <start>2024-02-11T23:00Z</start>
                      <end>2024-02-12T01:00Z</end>
                    </period.timeInterval>
                    <process.processType>A16</process.processType>
                    <TimeSeries>
                      <version>1</version>
                      <mRID>5713131791000XXXX1</mRID>
                      <businessType>A04</businessType>
                      <product>8716867000016</product>
                      <energy_Measurement_Unit.name>KWH</energy_Measurement_Unit.name>
                      <flowDirection.direction>A02</flowDirection.direction>
                      <Period>
                        <resolution>PT1H</resolution>
                        <timeInterval>
                          <start>2024-02-11T23:00Z</start>
                          <end>2024-02-12T01:00Z</end>
                        </timeInterval>
                        <Point>
                          <position>1</position>
                          <energy_Quantity.quantity>0.13</energy_Quantity.quantity>
                          <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                        <Point>
                            <position>2</position>
                            <energy_Quantity.quantity>0.11</energy_Quantity.quantity>
                            <energy_Quantity.quality>A04</energy_Quantity.quality>
                        </Point>
                        <reason.code>999</reason.code>
                      </Period>
                      <marketEvaluationPoint.mRID codingScheme="A10">5713131791000XXXX1</marketEvaluationPoint.mRID>
                      <marketEvaluationPoint.meterReadings.readings.readingType.commodity>2</marketEvaluationPoint.meterReadings.readings.readingType.commodity>
                      <reason.code>999</reason.code>
                    </TimeSeries>
                  </MarketDocument>
                </VHD_Envelope>
                """;
        var response = DtoLoader.loadValidatedHistoricalData();
        var pr = getPermissionRequest();
        var intermediateVHD = new IntermediateValidatedHistoricalDataMarketDocument(
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "ep-id"),
                new IdentifiableApiResponse(pr, Objects.requireNonNull(response.getResult()).getFirst())
        );

        // When
        var res = intermediateVHD.value();

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

    private static EnerginetPermissionRequest getPermissionRequest() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return new EnerginetPermissionRequest("pid",
                                              "cid",
                                              "dnid",
                                              "mid",
                                              "token",
                                              now.toLocalDate(),
                                              now.toLocalDate(),
                                              Granularity.P1D,
                                              null,
                                              PermissionProcessStatus.ACCEPTED,
                                              now,
                                              null);
    }

    private static Set<String> ignoredNames() {
        return Set.of(
                "messageDocumentHeader.creationDateTime",
                "dateAndOrTime.dateTime"
        );
    }
}