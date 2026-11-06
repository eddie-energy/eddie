// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatadisValidatedHistoricalDataMarketDocumentProviderTest {
    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_publishesDocuments() throws Exception {
        // Given
        EnergyDataStreams streams = new EnergyDataStreams();
        DatadisConfiguration datadisConfig = new DatadisConfiguration("clientId", "clientSecret", "basepath");
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                                                     "fallbackId");
        List<MeteringData> identifiableMeteringData = MeteringDataProvider.loadMeteringDataShort();
        var provider = new DatadisValidatedHistoricalDataMarketDocumentProvider(streams, datadisConfig, cimConfig);
        var pr = new DatadisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setMeteringPointId("mid")
                .setGranularity(Granularity.PT1H)
                .build();
        var intermediate = IntermediateMeteringData.fromMeteringData(identifiableMeteringData).block();

        // When
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> {
                        streams.publish(new IdentifiableMeteringData(pr, intermediate));
                        streams.close();
                    })
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_publishesDocuments_alsoEmitsEquivalentV112() throws Exception {
        // Given
        EnergyDataStreams streams = new EnergyDataStreams();
        DatadisConfiguration datadisConfig = new DatadisConfiguration("clientId", "clientSecret", "basepath");
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                                                     "fallbackId");
        List<MeteringData> identifiableMeteringData = MeteringDataProvider.loadMeteringDataShort();
        var provider = new DatadisValidatedHistoricalDataMarketDocumentProvider(streams, datadisConfig, cimConfig);
        var pr = new DatadisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setMeteringPointId("mid")
                .setGranularity(Granularity.PT1H)
                .build();
        var intermediate = IntermediateMeteringData.fromMeteringData(identifiableMeteringData).block();

        // When
        var pairedDocuments = Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(),
                                       provider.getValidatedHistoricalDataV112Stream());
        StepVerifier.create(pairedDocuments)
                    .then(() -> {
                        streams.publish(new IdentifiableMeteringData(pr, intermediate));
                        streams.close();
                    })
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
                assertEquals(3, sourcePeriod.getPoints().size(), "Fixture points per period");
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
