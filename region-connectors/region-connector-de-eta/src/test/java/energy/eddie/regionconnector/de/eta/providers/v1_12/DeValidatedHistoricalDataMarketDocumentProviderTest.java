// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.HistoricalDataInput;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
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
class DeValidatedHistoricalDataMarketDocumentProviderTest {
    @Mock
    private ValidatedHistoricalDataStream stream;

    private static final ZonedDateTime T0 = ZonedDateTime.of(2026, 4, 30, 22, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void mapsPopulatedRawFixtureWithoutLegacyProvider() {
        var publisher = TestPublisher.<IdentifiableValidatedHistoricalData>create();
        when(stream.historicalDataInputs()).thenReturn(publisher.flux().map(HistoricalDataInput::new).share());

        var cimConfig = new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME, "fallback");
        var etaConfig = new DeEtaPlusConfiguration(
                        "ep-id", "http://api.url", "client-id", "client-secret",
                        "/meters/historical", "/meters/accounting-point", 30,
                        3, 2, true, false, null, null);

        var providerV112 = new DeValidatedHistoricalDataMarketDocumentProvider(stream, cimConfig, etaConfig);

        StepVerifier.create(providerV112.getValidatedHistoricalDataV112Stream())
                .then(() -> publisher.emit(samplePayload()))
                .assertNext(DeValidatedHistoricalDataMarketDocumentProviderTest::assertPopulatedDocument)
                .verifyComplete();
    }

    private static IdentifiableValidatedHistoricalData samplePayload() {
        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid").connectionId("cid").meteringPointId("mp")
                .start(LocalDate.of(2026, 4, 30)).end(LocalDate.of(2026, 5, 1))
                .granularity(Granularity.PT15M).energyType(EnergyType.ELECTRICITY)
                .status(PermissionProcessStatus.ACCEPTED).created(T0).dataNeedId("dnid")
                .build();
        var reading = new EtaPlusMeteredData.MeterReading(T0, 1.0, "kWh", "VALIDATED", "Consumption");
        var payload = new EtaPlusMeteredData("mp", pr.start(), pr.end(), List.of(reading));
        return new IdentifiableValidatedHistoricalData(pr, payload);
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
