// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

class FluviusValidatedHistoricalDataMarketDocumentProviderTest {
    private final ClassLoader classLoader = this.getClass().getClassLoader();
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private FluviusOAuthConfiguration fluviusConfig;

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_gas_differentUnits() {
        // Given
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_quarter_hourly.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .dataNeedId("dnid")
                                                       .granularity(Granularity.P1D)
                                                       .build();

        // When
        var streams = new IdentifiableDataStreams();
        streams.publish(pr, json);
        streams.close();
        var provider = new FluviusValidatedHistoricalDataMarketDocumentProvider(fluviusConfig, streams);

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_gas_differentUnits_alsoEmitsEquivalentV112() {
        // Given
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_quarter_hourly.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .dataNeedId("dnid")
                                                       .granularity(Granularity.PT15M)
                                                       .build();

        // When
        var streams = new IdentifiableDataStreams();
        var provider = new FluviusValidatedHistoricalDataMarketDocumentProvider(new FluviusOAuthConfiguration("url", "client-id", "secret", "tenant", "scope"), streams);

        // Then
        var result = reactor.core.publisher.Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(), provider.getValidatedHistoricalDataV112Stream()).map(pair -> {
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
        }).collectList().toFuture();
        streams.publish(pr, json);
        streams.close();
        org.junit.jupiter.api.Assertions.assertEquals(1, result.join().size());
    }
}