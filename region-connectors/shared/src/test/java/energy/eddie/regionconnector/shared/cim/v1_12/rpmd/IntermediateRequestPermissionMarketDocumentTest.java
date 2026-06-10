// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12.rpmd;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.agnostic.DataSourceInformation;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.agnostic.SimpleDataSourceInformation;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v1_12.LocalCodingSchemeType;
import energy.eddie.cim.v1_12.StandardCodingSchemeTypeList;
import energy.eddie.cim.v1_12.StandardProcessTypeList;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.builder.DiffBuilder;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntermediateRequestPermissionMarketDocumentTest {
    @ParameterizedTest
    @MethodSource
    void toPermissionMarketDocument_mapsSuccessfully(
            String countryCode,
            String codingScheme
    ) throws SerdeInitializationException, SerializationException {
        // Given
        Clock clock = Clock.fixed(Instant.now(Clock.systemUTC()), ZoneOffset.UTC);
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(10);
        LocalDate end = today.minusDays(5);
        var timeInterval = new EsmpTimeInterval(start, end, ZoneOffset.UTC);


        var permissionRequest = mock(PermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn("pid", "pid");
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.created()).thenReturn(now);
        var dataSourceInformation = new SimpleDataSourceInformation(countryCode, "rc", "mda", "paID");
        when(permissionRequest.dataSourceInformation()).thenReturn(dataSourceInformation);
        when(permissionRequest.start()).thenReturn(start);
        when(permissionRequest.end()).thenReturn(end);
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        IntermediateRequestPermissionMarketDocument<PermissionRequest> csm = new IntermediateRequestPermissionMarketDocument<>(
                permissionRequest,
                "customerId",
                ignored -> Granularity.PT15M.name(),
                "NAT",
                ZoneOffset.UTC,
                new AccountingPointDataNeed(),
                PermissionProcessStatus.ACCEPTED
        );
        var ignoredNames = Set.of("mRID");
        var serde = new XmlMessageSerde();
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <RequestPermission_Envelope xmlns="https://eddie.energy/CEEDS_RequestPermissionDocument_annotated_v1.12.xsd">
                  <MarketDocument>
                    <mRID>pid</mRID>
                    <description>dnid</description>
                    <revisionNumber>112</revisionNumber>
                    <type>B48</type>
                    <sender_MarketParticipant.mRID codingScheme="%s">customerId</sender_MarketParticipant.mRID>
                    <sender_MarketParticipant.marketRole.type>A20</sender_MarketParticipant.marketRole.type>
                    <receiver_MarketParticipant.mRID codingScheme="%s">paID</receiver_MarketParticipant.mRID>
                    <receiver_MarketParticipant.marketRole.type>A59</receiver_MarketParticipant.marketRole.type>
                    <process.processType>A55</process.processType>
                    <period.timeInterval>
                      <start>%s</start>
                      <end>%s</end>
                    </period.timeInterval>
                    <Request_Permission>
                        <mRID>pid</mRID>
                        <createdDateTime>%s</createdDateTime>
                        <transmissionSchedule>P0Y0M0DT0H15M0.000S</transmissionSchedule>
                        <AccountingPoint><mRID codingScheme="NAT">cid</mRID></AccountingPoint>
                        <MktActivityRecord>
                            <mRID>mird</mRID>
                            <createdDateTime>%s</createdDateTime>
                            <description>ACCEPTED</description>
                            <type>rc</type>
                            <status>A37</status>
                        </MktActivityRecord>
                    </Request_Permission>
                  </MarketDocument>
                  <MessageDocumentHeader>
                    <creationDateTime>%s</creationDateTime>
                    <MetaInformation>
                      <connectionId>cid</connectionId>
                      <requestPermissionId>pid</requestPermissionId>
                      <dataNeedId>dnid</dataNeedId>
                      <documentType>request-permission-market-document</documentType>
                      <regionConnector>rc</regionConnector>
                      <regionCountry>%s</regionCountry>
                    </MetaInformation>
                  </MessageDocumentHeader>
                </RequestPermission_Envelope>
                """.formatted(codingScheme,
                              codingScheme,
                              timeInterval.start(),
                              timeInterval.end(),
                              new EsmpDateTime(now).toString(),
                              new EsmpDateTime(now).toString(),
                              new EsmpDateTime(now).toString(),
                              countryCode);

        // When
        var res = csm.toPermissionMarketDocument(clock);

        // Then
        var xml = serde.serialize(res);
        assertTrue(XmlValidator.validateV112RequestPermissionMarketDocument(xml));
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(xml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> ignoredNames.stream().noneMatch(node.getNodeName()::endsWith))
                                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription() + "\n" + new String(xml, StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @MethodSource
    void toPermissionMarketDocument_respectsDataNeed(DataNeed dataNeed, StandardProcessTypeList processType) {
        // Given
        var clock = Clock.fixed(Instant.now(Clock.systemUTC()), ZoneOffset.UTC);
        var now = ZonedDateTime.now(clock);
        var today = LocalDate.now(clock);
        var start = today.minusDays(10);
        var end = today.minusDays(5);

        var dataSourceInformation = mock(DataSourceInformation.class);
        when(dataSourceInformation.countryCode()).thenReturn("AT");
        when(dataSourceInformation.permissionAdministratorId()).thenReturn("paID");
        when(dataSourceInformation.regionConnectorId()).thenReturn("rc");

        var permissionRequest = mock(PermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn("pid", "pid");
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.created()).thenReturn(now);
        when(permissionRequest.dataSourceInformation()).thenReturn(dataSourceInformation);
        when(permissionRequest.start()).thenReturn(start);
        when(permissionRequest.end()).thenReturn(end);
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        IntermediateRequestPermissionMarketDocument<PermissionRequest> csm = new IntermediateRequestPermissionMarketDocument<>(
                permissionRequest,
                "customerId",
                ignored -> Granularity.PT15M.name(),
                "NAT",
                ZoneOffset.UTC,
                dataNeed,
                PermissionProcessStatus.ACCEPTED
        );

        // When
        var res = csm.toPermissionMarketDocument(clock);

        // Then
        var pmd = res.getMarketDocument();
        assertEquals(processType.value(), pmd.getProcessProcessType());
    }

    private static Stream<Arguments> toPermissionMarketDocument_mapsSuccessfully() {
        return Stream.of(
                Arguments.of("AT", StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value()),
                Arguments.of("DK", StandardCodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME.value()),
                Arguments.of("AIIDA", LocalCodingSchemeType.AIIDA.value())
        );
    }

    private static Stream<Arguments> toPermissionMarketDocument_respectsDataNeed() {
        return Stream.of(
                Arguments.of(new AccountingPointDataNeed(), StandardProcessTypeList.EXCHANGE_OF_MASTER_DATA),
                Arguments.of(new OutboundAiidaDataNeed(), StandardProcessTypeList.ACCESS_TO_METERED_DATA),
                Arguments.of(new ValidatedHistoricalDataDataNeed(new RelativeDuration(Period.ofYears(1),
                                                                                      Period.ofYears(1),
                                                                                      null),
                                                                 EnergyType.ELECTRICITY,
                                                                 Granularity.PT5M,
                                                                 Granularity.P1Y),
                             StandardProcessTypeList.ACCESS_TO_METERED_DATA)
        );
    }
}