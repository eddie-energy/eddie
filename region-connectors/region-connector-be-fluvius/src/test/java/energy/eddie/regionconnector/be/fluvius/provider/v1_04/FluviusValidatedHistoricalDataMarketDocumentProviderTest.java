// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void quarterHourlyElectricityEmitsEquivalentV112(boolean subscribeV112First) {
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
        var config = new FluviusOAuthConfiguration("url", "client-id", "secret", "tenant", "scope");
        var provider = new FluviusValidatedHistoricalDataMarketDocumentProvider(config, streams);

        // Then
        var providerV112 = new energy.eddie.regionconnector.be.fluvius.provider.v1_12.FluviusValidatedHistoricalDataMarketDocumentProvider(config, streams);
        var pairedDocuments = subscribeV112First
                ? Flux.zip(providerV112.getValidatedHistoricalDataV112Stream(), provider.getValidatedHistoricalDataMarketDocumentsStream())
                      .map(pair -> reactor.util.function.Tuples.of(pair.getT2(), pair.getT1()))
                : Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(), providerV112.getValidatedHistoricalDataV112Stream());
        StepVerifier.create(pairedDocuments)
                    .then(() -> streams.publish(pr, json))
                    .assertNext(pair -> assertEquivalentHistoricalData(pair.getT1(), pair.getT2()))
                    .then(streams::close)
                    .verifyComplete();
    }

    private static void assertEquivalentHistoricalData(VHDEnvelope source, Envelope target) {
        assertEquals(source.getMarketDocument().getMRID(), target.getVHDMarketDocument().getMRID());
        assertEquals(source.getMarketDocument().getCreatedDateTime(), target.getVHDMarketDocument().getCreatedDateTime());
        assertEquals(source.getMarketDocument().getRevisionNumber(), target.getVHDMarketDocument().getRevisionNumber());
        assertEquals(source.getMessageDocumentHeaderCreationDateTime(), target.getMessageDocumentHeader().getCreationDateTime());
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
        assertEquals(2, sourceSeries.size(), "Fixture time series");
        assertEquals(sourceSeries.size(), targetSeries.size());
        for (int i = 0; i < sourceSeries.size(); i++) {
            var sourceTimeSeries = sourceSeries.get(i);
            var targetTimeSeries = targetSeries.get(i);
            assertEquals(sourceTimeSeries.getMRID(), targetTimeSeries.getMRID());
            assertEquals(sourceTimeSeries.getBusinessType(), targetTimeSeries.getBusinessType());
            assertEquals(sourceTimeSeries.getProduct(), targetTimeSeries.getProduct());
            assertEquals(sourceTimeSeries.getMarketEvaluationPointMRID().getValue(), targetTimeSeries.getAccountingPointMRID().getValue());
            assertEquals(sourceTimeSeries.getMarketEvaluationPointMRID().getCodingScheme(), targetTimeSeries.getAccountingPointMRID().getCodingScheme());
            assertEquals(source.getMarketDocument().getCreatedDateTime(), targetTimeSeries.getDateAndOrTimeDateTime());
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
                assertEquals(1, sourcePeriod.getPoints().size(), "Fixture points per period");
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
            var xmlRoundTrip = new XmlMessageSerde().deserialize(serialized, Envelope.class);
            var json = new JsonMessageSerde();
            var jsonRoundTrip = json.deserialize(json.serialize(target), Envelope.class);
            assertEquals(target.getVHDMarketDocument().getMRID(), xmlRoundTrip.getVHDMarketDocument().getMRID());
            assertEquals(target.getVHDMarketDocument().getMRID(), jsonRoundTrip.getVHDMarketDocument().getMRID());
            assertEquals(target.getVHDMarketDocument().getTimeSeries().size(), xmlRoundTrip.getVHDMarketDocument().getTimeSeries().size());
            assertEquals(target.getVHDMarketDocument().getTimeSeries().size(), jsonRoundTrip.getVHDMarketDocument().getTimeSeries().size());
        });
    }
}
