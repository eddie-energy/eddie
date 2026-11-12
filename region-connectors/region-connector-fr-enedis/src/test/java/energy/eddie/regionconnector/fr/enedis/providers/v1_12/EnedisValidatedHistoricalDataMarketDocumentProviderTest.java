// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequestBuilder;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class EnedisValidatedHistoricalDataMarketDocumentProviderTest {
    @Test
    void mapsPopulatedRawFixtureWithoutLegacyProvider() throws Exception {
        var meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK);
        var permissionRequest = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setGranularity(Granularity.P1D)
                .create();

        var identifiableMeterReading = new IdentifiableMeterReading(permissionRequest,
                                                                    meterReading,
                                                                    MeterReadingType.CONSUMPTION);
        EnedisConfiguration enedisConfiguration = new EnedisConfiguration(
                "clientId",
                "clientSecret",
                "/path"
        );
        EnergyDataStreams streams = new EnergyDataStreams();

        var providerV112 = new EnedisValidatedHistoricalDataMarketDocumentProvider(streams, enedisConfiguration);

        StepVerifier.create(providerV112.getValidatedHistoricalDataV112Stream())
                    .then(() -> {
                        streams.publish(identifiableMeterReading);
                        streams.close();
                    })
                    .assertNext(EnedisValidatedHistoricalDataMarketDocumentProviderTest::assertPopulatedDocument)
                    .verifyComplete();
    }

    private static void assertPopulatedDocument(Envelope envelope) {
        assertEquals("pid", envelope.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
        assertEquals("dnid", envelope.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
        var document = envelope.getVHDMarketDocument();
        assertNotNull(document);
        assertFalse(document.getTimeSeries().isEmpty());
        var pointCount = 0;
        for (var series : document.getTimeSeries()) {
            assertNotNull(series.getDateAndOrTimeDateTime());
            assertFalse(series.getSeriesPeriods().isEmpty());
            for (var period : series.getSeriesPeriods()) {
                assertNotNull(period.getResolution());
                assertNotNull(period.getTimeInterval());
                assertFalse(period.getPoints().isEmpty());
                for (var point : period.getPoints()) {
                    assertTrue(point.getPosition() > 0);
                    assertTrue(point.getEnergyQuantityQuantity() != null || point.getEnergyQualityQuantityQuantity() != null);
                    pointCount++;
                }
            }
        }
        assertTrue(pointCount > 0);
    }
}
