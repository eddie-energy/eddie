// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import energy.eddie.regionconnector.fi.fingrid.services.HistoricalDataInput;
import energy.eddie.regionconnector.fi.fingrid.services.cim.IdentifiableValidatedHistoricalData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CimStreamsTest {
    @Mock
    private EnergyDataService energyDataService;

    @Test
    void mapsPopulatedRawFixtureWithoutLegacyProvider() {
        var publisher = TestPublisher.<IdentifiableValidatedHistoricalData>create();
        when(energyDataService.historicalDataInputs()).thenReturn(publisher.flux().map(HistoricalDataInput::new).share());
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(LocalDate.now(ZoneOffset.UTC))
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT15M)
                                                      .setLastMeterReadings(null)
                                                      .build();
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);

        var providerV112 = new CimStreams(energyDataService);

        StepVerifier.create(providerV112.getValidatedHistoricalDataV112Stream())
                    .then(() -> publisher.emit(new IdentifiableValidatedHistoricalData(pr, List.of(resp))))
                    .then(publisher::complete)
                    .assertNext(CimStreamsTest::assertPopulatedDocument)
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
