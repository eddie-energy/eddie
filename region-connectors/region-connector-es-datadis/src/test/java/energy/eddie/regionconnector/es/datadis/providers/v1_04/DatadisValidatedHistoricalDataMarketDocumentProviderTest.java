// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
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
        StepVerifier.create(reactor.core.publisher.Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(), provider.getValidatedHistoricalDataV112Stream()).map(pair -> {
            var source = pair.getT1();
            var target = pair.getT2();
            org.junit.jupiter.api.Assertions.assertEquals(source.getMessageDocumentHeaderMetaInformationPermissionId(), target.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
            org.junit.jupiter.api.Assertions.assertEquals(source.getMessageDocumentHeaderMetaInformationDataNeedId(), target.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
            var sourceSeries = source.getMarketDocument().getTimeSeries();
            var targetSeries = target.getVHDMarketDocument().getTimeSeries();
            org.junit.jupiter.api.Assertions.assertEquals(sourceSeries.size(), targetSeries.size());
            for (int i = 0; i < sourceSeries.size(); i++) {
                var left = sourceSeries.get(i);
                var right = targetSeries.get(i);
                org.junit.jupiter.api.Assertions.assertEquals(left.getEnergyMeasurementUnitName(), right.getEnergyMeasurementUnitName());
                org.junit.jupiter.api.Assertions.assertEquals(left.getEnergyQualityMeasurementUnitName(), right.getEnergyQualityMeasurementUnitName());
                org.junit.jupiter.api.Assertions.assertEquals(left.getFlowDirectionDirection(), right.getFlowDirectionDirection());
                org.junit.jupiter.api.Assertions.assertEquals(left.getPeriods().size(), right.getSeriesPeriods().size());
                for (int j = 0; j < left.getPeriods().size(); j++) {
                    var lp = left.getPeriods().get(j);
                    var rp = right.getSeriesPeriods().get(j);
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getResolution(), rp.getResolution());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getTimeInterval().getStart(), rp.getTimeInterval().getStart());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getTimeInterval().getEnd(), rp.getTimeInterval().getEnd());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getPoints().size(), rp.getPoints().size());
                    for (int k = 0; k < lp.getPoints().size(); k++) {
                        var l = lp.getPoints().get(k);
                        var r = rp.getPoints().get(k);
                        org.junit.jupiter.api.Assertions.assertEquals(l.getPosition(), r.getPosition());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQuantityQuantity(), r.getEnergyQuantityQuantity());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQuantityQuality(), r.getEnergyQuantityQuality());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQualityQuantityQuantity(), r.getEnergyQualityQuantityQuantity());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQualityQuantityQuality(), r.getEnergyQualityQuantityQuality());
                    }
                }
            }
            if (!targetSeries.isEmpty()) {
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                    var serialized = new energy.eddie.cim.serde.XmlMessageSerde().serialize(target);
                    org.junit.jupiter.api.Assertions.assertTrue(energy.eddie.cim.testing.XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(serialized));
                });
            }
            return target;
        }))
                    .then(() -> {
                        streams.publish(new IdentifiableMeteringData(pr, intermediate));
                        streams.close();
                    })
                    .expectNextCount(1)
                    .verifyComplete();
    }
}