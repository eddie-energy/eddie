// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class FluviusValidatedHistoricalDataMarketDocumentProviderTest {
    private final ClassLoader classLoader = this.getClass().getClassLoader();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsPopulatedRawFixtureWithoutLegacyProvider() {
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_quarter_hourly.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .dataNeedId("dnid")
                                                       .granularity(Granularity.PT15M)
                                                       .build();

        var config = new FluviusOAuthConfiguration("url", "client-id", "secret", "tenant", "scope");
        var provider = new FluviusValidatedHistoricalDataMarketDocumentProvider(config, streams);

        var providerV112 = new FluviusValidatedHistoricalDataMarketDocumentProvider(config, streams);

        StepVerifier.create(providerV112.getValidatedHistoricalDataV112Stream())
                    .then(() -> streams.publish(pr, json))
                    .assertNext(FluviusValidatedHistoricalDataMarketDocumentProviderTest::assertPopulatedDocument)
                    .then(streams::close)
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
