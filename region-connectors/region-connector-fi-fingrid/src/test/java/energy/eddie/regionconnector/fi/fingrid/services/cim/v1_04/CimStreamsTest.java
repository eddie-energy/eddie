// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import energy.eddie.regionconnector.fi.fingrid.services.cim.IdentifiableValidatedHistoricalData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
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
    void publishEmitsVHD() {
        // Given
        var publisher = TestPublisher.<IdentifiableValidatedHistoricalData>create();
        when(energyDataService.getIdentifiableValidatedHistoricalDataStream()).thenReturn(publisher.flux());
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
        var streams = new CimStreams(energyDataService);

        // When
        StepVerifier.create(streams.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> publisher.emit(new IdentifiableValidatedHistoricalData(pr, List.of(resp))))
                    .then(publisher::complete)
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void publishEmitsVHD_alsoEmitsEquivalentV112() {
        // Given
        var publisher = TestPublisher.<IdentifiableValidatedHistoricalData>create();
        when(energyDataService.getIdentifiableValidatedHistoricalDataStream()).thenReturn(publisher.flux());
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
        var streams = new CimStreams(energyDataService);

        // When
        var pairedDocuments = Flux.zip(streams.getValidatedHistoricalDataMarketDocumentsStream(),
                                       streams.getValidatedHistoricalDataV112Stream());
        StepVerifier.create(pairedDocuments)
                    .then(() -> publisher.emit(new IdentifiableValidatedHistoricalData(pr, List.of(resp))))
                    .then(publisher::complete)
                    // Then
                    .assertNext(pair -> assertEquivalentHistoricalData(pair.getT1(), pair.getT2()))
                    .verifyComplete();
    }

    private static void assertEquivalentHistoricalData(VHDEnvelope source, Envelope target) {
        assertEquals(source.getMarketDocument().getMRID(), target.getVHDMarketDocument().getMRID());
        assertEquals(
                source.getMessageDocumentHeaderMetaInformationPermissionId(),
                target.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId()
        );
        assertEquals(
                source.getMessageDocumentHeaderMetaInformationDataNeedId(),
                target.getMessageDocumentHeader().getMetaInformation().getDataNeedId()
        );
        var sourceSeries = source.getMarketDocument().getTimeSeries();
        var targetSeries = target.getVHDMarketDocument().getTimeSeries();
        assertEquals(1, sourceSeries.size(), "Fixture time series");
        assertEquals(sourceSeries.size(), targetSeries.size());
        for (int i = 0; i < sourceSeries.size(); i++) {
            var sourceTimeSeries = sourceSeries.get(i);
            var targetTimeSeries = targetSeries.get(i);
            assertEquals(sourceTimeSeries.getMRID(), targetTimeSeries.getMRID());
            assertEquals(
                    sourceTimeSeries.getEnergyMeasurementUnitName(),
                    targetTimeSeries.getEnergyMeasurementUnitName()
            );
            assertEquals(
                    sourceTimeSeries.getEnergyQualityMeasurementUnitName(),
                    targetTimeSeries.getEnergyQualityMeasurementUnitName()
            );
            assertEquals(sourceTimeSeries.getFlowDirectionDirection(), targetTimeSeries.getFlowDirectionDirection());
            assertEquals(1, sourceTimeSeries.getPeriods().size(), "Fixture periods per time series");
            assertEquals(sourceTimeSeries.getPeriods().size(), targetTimeSeries.getSeriesPeriods().size());
            for (int j = 0; j < sourceTimeSeries.getPeriods().size(); j++) {
                var sourcePeriod = sourceTimeSeries.getPeriods().get(j);
                var targetPeriod = targetTimeSeries.getSeriesPeriods().get(j);
                assertEquals(sourcePeriod.getResolution(), targetPeriod.getResolution());
                assertEquals(sourcePeriod.getTimeInterval().getStart(), targetPeriod.getTimeInterval().getStart());
                assertEquals(sourcePeriod.getTimeInterval().getEnd(), targetPeriod.getTimeInterval().getEnd());
                assertEquals(2, sourcePeriod.getPoints().size(), "Fixture points per period");
                assertEquals(sourcePeriod.getPoints().size(), targetPeriod.getPoints().size());
                for (int k = 0; k < sourcePeriod.getPoints().size(); k++) {
                    var sourcePoint = sourcePeriod.getPoints().get(k);
                    var targetPoint = targetPeriod.getPoints().get(k);
                    assertEquals(sourcePoint.getPosition(), targetPoint.getPosition());
                    assertEquals(sourcePoint.getEnergyQuantityQuantity(), targetPoint.getEnergyQuantityQuantity());
                    assertEquals(sourcePoint.getEnergyQuantityQuality(), targetPoint.getEnergyQuantityQuality());
                    assertEquals(
                            sourcePoint.getEnergyQualityQuantityQuantity(),
                            targetPoint.getEnergyQualityQuantityQuantity()
                    );
                    assertEquals(
                            sourcePoint.getEnergyQualityQuantityQuality(),
                            targetPoint.getEnergyQualityQuantityQuality()
                    );
                }
            }
        }
        assertDoesNotThrow(() -> {
            var serialized = new XmlMessageSerde().serialize(target);
            assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(serialized));
        });
    }
}
