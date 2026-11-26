// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.de.eta.providers.v1_04.DeValidatedHistoricalDataMarketDocumentProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeValidatedHistoricalDataMarketDocumentProviderV112Test {
    private static final ZonedDateTime T0 = ZonedDateTime.of(2026, 4, 30, 22, 0, 0, 0, ZoneOffset.UTC);
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME, "fallback");
    private final DeEtaPlusConfiguration deConfiguration = new DeEtaPlusConfiguration(
            "ep-id", "http://api.url", "client-id", "client-secret",
            "/meters/historical", "/meters/accounting-point", 30,
            3, 2, true, false, null, null);
    private final XmlMessageSerde serde = new XmlMessageSerde();

    DeValidatedHistoricalDataMarketDocumentProviderV112Test() throws SerdeInitializationException {}

    @ParameterizedTest
    @MethodSource("payloads")
    void getValidatedHistoricalDataMarketDocumentsStream_isEquivalentToV104(
            IdentifiableValidatedHistoricalData payload,
            int expectedDocuments
    ) throws SerializationException {
        // Given
        var stream = mock(ValidatedHistoricalDataStream.class);
        when(stream.validatedHistoricalData()).thenReturn(Flux.just(payload));
        var v104Provider = new DeValidatedHistoricalDataMarketDocumentProvider(stream, cimConfig, deConfiguration);
        var v112Provider = new DeValidatedHistoricalDataMarketDocumentProviderV112(stream, cimConfig, deConfiguration);

        // When
        var v104 = v104Provider.getValidatedHistoricalDataMarketDocumentsStream().collectList().block();
        var v112 = v112Provider.getValidatedHistoricalDataMarketDocumentsStream().collectList().block();

        // Then
        assertNotNull(v104);
        assertNotNull(v112);
        assertEquals(expectedDocuments, v104.size());
        assertEquals(expectedDocuments, v112.size());
        for (int i = 0; i < v104.size(); i++) {
            // The creation time is taken from the clock at mapping time, so it differs between versions
            assertEquals(List.of(),
                         ValidatedHistoricalDataVersionComparator.differencesIgnoring(v104.get(i),
                                                                                      v112.get(i),
                                                                                      "createdDateTime"));
            var xml = serde.serialize(v112.get(i));
            assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                       "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
            assertEquals("DE", v112.get(i).getMessageDocumentHeader().getMetaInformation().getRegionCountry());
        }
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void getValidatedHistoricalDataMarketDocumentsStream_emitsEnvelopes(
            IdentifiableValidatedHistoricalData payload,
            int expectedDocuments
    ) {
        // Given
        var stream = mock(ValidatedHistoricalDataStream.class);
        when(stream.validatedHistoricalData()).thenReturn(Flux.just(payload));
        var provider = new DeValidatedHistoricalDataMarketDocumentProviderV112(stream, cimConfig, deConfiguration);

        // When & Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .expectNextCount(expectedDocuments)
                    .verifyComplete();
    }

    private static Stream<Arguments> payloads() {
        return Stream.of(
                Arguments.of(payload(EnergyType.ELECTRICITY,
                                     List.of(new EtaPlusMeteredData.MeterReading(T0, 1.0, "kWh", "VALIDATED", "Consumption"),
                                             new EtaPlusMeteredData.MeterReading(T0.plusMinutes(15), 2.0, "kWh", "unknown", "Consumption"))),
                             1),
                Arguments.of(payload(EnergyType.NATURAL_GAS,
                                     List.of(new EtaPlusMeteredData.MeterReading(T0, 1.0, "m3", "VALIDATED", "Production"))),
                             1),
                Arguments.of(payload(EnergyType.ELECTRICITY,
                                     List.of(new EtaPlusMeteredData.MeterReading(T0, 1.0, "unsupported", "VALIDATED", "Consumption"))),
                             0),
                Arguments.of(payload(EnergyType.ELECTRICITY, List.of()), 0)
        );
    }

    private static IdentifiableValidatedHistoricalData payload(
            EnergyType energyType,
            List<EtaPlusMeteredData.MeterReading> readings
    ) {
        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid").connectionId("cid").meteringPointId("mp")
                .start(LocalDate.of(2026, 4, 30)).end(LocalDate.of(2026, 5, 1))
                .granularity(Granularity.PT15M).energyType(energyType)
                .status(PermissionProcessStatus.ACCEPTED).created(T0).dataNeedId("dnid")
                .build();
        var payload = new EtaPlusMeteredData("mp", pr.start(), pr.end(), readings);
        return new IdentifiableValidatedHistoricalData(pr, payload);
    }
}
