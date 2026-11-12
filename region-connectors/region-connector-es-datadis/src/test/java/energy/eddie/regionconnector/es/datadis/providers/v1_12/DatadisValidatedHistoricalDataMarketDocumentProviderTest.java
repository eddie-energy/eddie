// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatadisValidatedHistoricalDataMarketDocumentProviderTest {
    @Test
    void mapsPopulatedRawFixtureWithoutLegacyProvider() throws Exception {
        EnergyDataStreams streams = new EnergyDataStreams();
        DatadisConfiguration datadisConfig = new DatadisConfiguration("clientId", "clientSecret", "basepath");
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                                                     "fallbackId");
        List<MeteringData> identifiableMeteringData = MeteringDataProvider.loadMeteringDataShort();

        var pr = new DatadisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setMeteringPointId("mid")
                .setGranularity(Granularity.PT1H)
                .build();
        var intermediate = IntermediateMeteringData.fromMeteringData(identifiableMeteringData).block();

        var providerV112 = new DatadisValidatedHistoricalDataMarketDocumentProvider(streams, datadisConfig, cimConfig);

        StepVerifier.create(providerV112.getValidatedHistoricalDataV112Stream())
                    .then(() -> {
                        streams.publish(new IdentifiableMeteringData(pr, intermediate));
                        streams.close();
                    })
                    .assertNext(DatadisValidatedHistoricalDataMarketDocumentProviderTest::assertPopulatedDocument)
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
