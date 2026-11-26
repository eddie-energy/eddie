// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequestBuilder;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import energy.eddie.regionconnector.fr.enedis.providers.v1_04.EnedisValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EnedisValidatedHistoricalDataMarketDocumentProviderV112Test {
    private final EnedisConfiguration enedisConfiguration = new EnedisConfiguration("clientId", "clientSecret", "/path");

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_publishesDocuments() throws Exception {
        // Given
        var reading = identifiableMeterReading(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK, Granularity.P1D);
        var streams = new EnergyDataStreams();
        var provider = new EnedisValidatedHistoricalDataMarketDocumentProviderV112(streams, enedisConfiguration);

        // When & Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> {
                        streams.publish(reading);
                        streams.close();
                    })
                    .assertNext(vhd -> assertEquals("pid",
                                                    vhd.getMessageDocumentHeader()
                                                       .getMetaInformation()
                                                       .getRequestPermissionId()))
                    .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void testGetValidatedHistoricalDataMarketDocumentsStream_isEquivalentToV104AndValid(
            String file,
            Granularity granularity
    ) throws Exception {
        // Given
        var streams = new EnergyDataStreams();
        var v104Future = new EnedisValidatedHistoricalDataMarketDocumentProvider(streams, enedisConfiguration)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();
        var v112Future = new EnedisValidatedHistoricalDataMarketDocumentProviderV112(streams, enedisConfiguration)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();

        // When
        streams.publish(identifiableMeterReading(file, granularity));
        streams.close();

        // Then
        var v104 = v104Future.get();
        var v112 = v112Future.get();
        assertNotNull(v104);
        assertNotNull(v112);
        assertEquals(1, v104.size());
        assertEquals(1, v112.size());
        // The creation time is taken from the clock at mapping time, so it differs between versions
        assertEquals(List.of(),
                     ValidatedHistoricalDataVersionComparator.differencesIgnoring(v104.getFirst(),
                                                                                  v112.getFirst(),
                                                                                  "createdDateTime"));
        var envelope = v112.getFirst();
        assertEquals("FR", envelope.getMessageDocumentHeader().getMetaInformation().getRegionCountry());
        var xml = new XmlMessageSerde().serialize(envelope);
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                   "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
    }

    private static IdentifiableMeterReading identifiableMeterReading(String file, Granularity granularity) throws Exception {
        var meterReading = TestResourceProvider.readMeterReadingFromFile(file);
        var permissionRequest = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setGranularity(granularity)
                .create();
        return new IdentifiableMeterReading(permissionRequest, meterReading, MeterReadingType.CONSUMPTION);
    }

    private static Stream<Arguments> fixtures() {
        return Stream.of(
                Arguments.of(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK, Granularity.P1D),
                Arguments.of(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY, Granularity.PT30M),
                Arguments.of(TestResourceProvider.CONSUMPTION_LOAD_CURVE_WITH_CHANGING_RESOLUTION_1_DAY,
                             Granularity.PT30M)
        );
    }
}
