// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.ApiMetaData;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.Headpoint;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDocumentTest {
    private final ClassLoader classLoader = this.getClass().getClassLoader();
    private final ObjectMapper mapper = new ObjectMapper();

    private final FluviusOAuthConfiguration fluviusConfig = new FluviusOAuthConfiguration("http://localhost:8080",
                                                                                          "client-id",
                                                                                          "client-secret",
                                                                                          "tenant-id",
                                                                                          "scope");

    @ParameterizedTest
    @MethodSource("testToVhd")
    void testToVhd(
            String inputJsonFile,
            Granularity granularity
    ) {
        var json = classLoader.getResourceAsStream(inputJsonFile);
        var payload = mapper.readValue(json, GetEnergyResponseModelApiDataResponse.class);
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .connectionId("cid")
                                                       .dataNeedId("dnid")
                                                       .granularity(granularity)
                                                       .build();
        var doc = new IntermediateValidatedHistoricalDocument(fluviusConfig, new IdentifiableMeteringData(pr, payload));

        var res = doc.toVHD();

        assertNotNull(res);
        assertHistoricalDataRoundTrips(res);
    }

    private static Stream<Arguments> testToVhd() {
        return Stream.of(
                Arguments.of(
                        "electricity_data_measurement_quarter_hourly.json",
                        Granularity.PT15M
                ),
                Arguments.of(
                        "electricity_data_headpoints_measurement_quarter_hourly.json",
                        Granularity.PT15M
                ),
                Arguments.of(
                        "electricity_data_headpoints_and_physical_meters_measurement_quarter_hourly.json",
                        Granularity.PT15M
                ),
                Arguments.of(
                        "gas_data_measurement_hourly.json",
                        Granularity.PT1H
                )
        );
    }

    @Test
    void suppressesMissingHeadpointAndPreservesHeaderOnlyResultForEmptyReadings() {
        var permission = DefaultFluviusPermissionRequestBuilder.create()
                .permissionId("pid").connectionId("cid").dataNeedId("dnid").granularity(Granularity.PT15M).build();
        var absent = new GetEnergyResponseModelApiDataResponse(new ApiMetaData(null), new GetEnergyResponseModel(null));
        var empty = new GetEnergyResponseModelApiDataResponse(new ApiMetaData(null),
                new GetEnergyResponseModel(new Headpoint("meter", EnergyType.ELECTRICITY, List.of())));

        assertNull(new IntermediateValidatedHistoricalDocument(fluviusConfig,
                new IdentifiableMeteringData(permission, absent)).toVHD());
        var result = new IntermediateValidatedHistoricalDocument(fluviusConfig,
                new IdentifiableMeteringData(permission, empty)).toVHD();

        assertNotNull(result);
        assertNull(result.getVHDMarketDocument());
        assertEquals("pid", result.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
    }

    private static void assertHistoricalDataRoundTrips(Envelope document) {
        assertNotNull(document.getVHDMarketDocument());
        assertFalse(document.getVHDMarketDocument().getTimeSeries().isEmpty());
        var xml = assertDoesNotThrow(XmlMessageSerde::new);
        var serialized = assertDoesNotThrow(() -> xml.serialize(document));
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(serialized));
        var xmlRoundTrip = assertDoesNotThrow(() -> xml.deserialize(serialized, Envelope.class));
        var json = new JsonMessageSerde();
        var jsonRoundTrip = assertDoesNotThrow(() -> json.deserialize(json.serialize(document), Envelope.class));
        assertEquals(document.getVHDMarketDocument().getMRID(), xmlRoundTrip.getVHDMarketDocument().getMRID());
        assertEquals(document.getVHDMarketDocument().getMRID(), jsonRoundTrip.getVHDMarketDocument().getMRID());
        var expectedSeries = document.getVHDMarketDocument().getTimeSeries();
        assertEquals(expectedSeries.size(), xmlRoundTrip.getVHDMarketDocument().getTimeSeries().size());
        assertEquals(expectedSeries.size(), jsonRoundTrip.getVHDMarketDocument().getTimeSeries().size());
        for (var index = 0; index < expectedSeries.size(); index++) {
            var expected = expectedSeries.get(index);
            assertFalse(expected.getSeriesPeriods().isEmpty());
            var fromXml = xmlRoundTrip.getVHDMarketDocument().getTimeSeries().get(index);
            var fromJson = jsonRoundTrip.getVHDMarketDocument().getTimeSeries().get(index);
            assertEquals(expected.getSeriesPeriods().size(), fromXml.getSeriesPeriods().size());
            assertEquals(expected.getSeriesPeriods().size(), fromJson.getSeriesPeriods().size());
            assertEquals(expected.getDateAndOrTimeDateTime().toInstant(), fromXml.getDateAndOrTimeDateTime().toInstant());
            assertEquals(expected.getDateAndOrTimeDateTime().toInstant(), fromJson.getDateAndOrTimeDateTime().toInstant());
            for (var period = 0; period < expected.getSeriesPeriods().size(); period++) {
                var points = expected.getSeriesPeriods().get(period).getPoints();
                assertFalse(points.isEmpty());
                var xmlPoints = fromXml.getSeriesPeriods().get(period).getPoints();
                var jsonPoints = fromJson.getSeriesPeriods().get(period).getPoints();
                assertEquals(points.size(), xmlPoints.size());
                assertEquals(points.size(), jsonPoints.size());
                for (var point = 0; point < points.size(); point++) {
                    assertEquals(points.get(point).getPosition(), xmlPoints.get(point).getPosition());
                    assertEquals(points.get(point).getPosition(), jsonPoints.get(point).getPosition());
                    assertEquals(points.get(point).getEnergyQuantityQuantity(), xmlPoints.get(point).getEnergyQuantityQuantity());
                    assertEquals(points.get(point).getEnergyQuantityQuantity(), jsonPoints.get(point).getEnergyQuantityQuantity());
                    assertEquals(points.get(point).getEnergyQualityQuantityQuantity(), xmlPoints.get(point).getEnergyQualityQuantityQuantity());
                    assertEquals(points.get(point).getEnergyQualityQuantityQuantity(), jsonPoints.get(point).getEnergyQualityQuantityQuantity());
                }
            }
        }
    }
}
