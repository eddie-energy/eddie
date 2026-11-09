// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.dk.energinet.DtoLoader;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class EnerginetValidatedHistoricalDataMarketDocumentProviderTest {

    @Test
    void givenValidatedHistoricalData_whenReceived_thenPublishValidatedHistoricalDataMarketDocument() throws IOException {
        // Given
        var streams = new EnergyDataStreams();
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "ep-id");
        var provider = new EnerginetValidatedHistoricalDataMarketDocumentProvider(streams, cimConfig);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new EnerginetPermissionRequest("pid",
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
        var response = DtoLoader.loadValidatedHistoricalData().getResult();

        // When
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> {
                        streams.publish(new IdentifiableApiResponse(pr, Objects.requireNonNull(response).getFirst()));
                        streams.close();
                    })
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void givenValidatedHistoricalData_whenReceived_thenPublishValidatedHistoricalDataMarketDocument_alsoEmitsEquivalentV112() throws IOException {
        // Given
        var streams = new EnergyDataStreams();
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "ep-id");
        var provider = new EnerginetValidatedHistoricalDataMarketDocumentProvider(streams, cimConfig);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new EnerginetPermissionRequest("pid",
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
        var response = DtoLoader.loadValidatedHistoricalData().getResult();

        // When
        var providerV112 = new energy.eddie.regionconnector.dk.energinet.providers.v1_12.EnerginetValidatedHistoricalDataMarketDocumentProvider(provider);
        var pairedDocuments = Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(),
                                       providerV112.getValidatedHistoricalDataV112Stream());
        StepVerifier.create(pairedDocuments)
                    .then(() -> {
                        streams.publish(new IdentifiableApiResponse(pr, Objects.requireNonNull(response).getFirst()));
                        streams.close();
                    })
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
