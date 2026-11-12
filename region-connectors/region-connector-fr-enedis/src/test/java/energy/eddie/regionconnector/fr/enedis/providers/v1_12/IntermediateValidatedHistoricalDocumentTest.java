// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v1_12;

import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequestBuilder;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDocumentTest {
    @Test
    void givenIdentifiableMeterReading_whenValue_thenReturnValidatedHistoricalDataMarketDocument() throws IOException {
        var response = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK);
        var pr = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .create();
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                new IdentifiableMeterReading(pr, response, MeterReadingType.CONSUMPTION),
                new EnedisConfiguration("client-id", "secret", "https://localhost")
        );

        var res = intermediateVHD.value();

        assertHistoricalDataRoundTrips(res);
    }

    @Test
    void preservesChangingResolutionPositionsScalingAndExistingVoltAmpereRepresentation() throws IOException {
        var fixture = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.DAILY_CONSUMPTION_1_WEEK);
        var start = ZonedDateTime.parse("2024-02-01T00:15Z");
        var points = List.of(
                new MeterReading.Point(1000.0, start, Optional.of(Duration.ofMinutes(15)), Optional.empty(), Optional.empty(), Optional.empty()),
                new MeterReading.Point(2000.0, start.plusMinutes(15), Optional.of(Duration.ofMinutes(15)), Optional.empty(), Optional.empty(), Optional.empty()),
                new MeterReading.Point(3000.0, start.plusMinutes(45), Optional.of(Duration.ofMinutes(30)), Optional.empty(), Optional.empty(), Optional.empty()));
        var readings = List.of(
                new MeterReading.Reading(MeterReading.Direction.CONSUMPTION, "energy", "Wh", points, null),
                new MeterReading.Reading(MeterReading.Direction.PRODUCTION, "power", "VA", List.of(points.getFirst()), null));
        var raw = new MeterReading(fixture.usagePointId(), fixture.meterReadingType(), fixture.period(),
                fixture.valueType(), fixture.calculationMode(), fixture.granularity(), readings, fixture.context());
        var permission = new EnedisPermissionRequestBuilder().setPermissionId("pid")
                .setConnectionId("cid").setDataNeedId("dnid").create();
        var input = new IdentifiableMeterReading(permission, raw, MeterReadingType.CONSUMPTION);
        var config = new EnedisConfiguration("client-id", "secret", "https://localhost");

        var series = new IntermediateValidatedHistoricalDocument(input, config).value().getVHDMarketDocument().getTimeSeries();

        assertEquals(2, series.size());
        var periods = series.getFirst().getSeriesPeriods();
        assertEquals(2, periods.size());
        assertEquals(List.of(1, 2), periods.getFirst().getPoints().stream().map(point -> point.getPosition()).toList());
        assertEquals(1, periods.getLast().getPoints().getFirst().getPosition());
        assertEquals(start.minusMinutes(15).toString(), periods.getFirst().getTimeInterval().getStart());
        assertEquals(start.plusMinutes(15).toString(), periods.getLast().getTimeInterval().getStart());
        assertEquals(1.0, periods.getFirst().getPoints().getFirst().getEnergyQuantityQuantity().doubleValue());
        assertEquals(3.0, periods.getLast().getPoints().getFirst().getEnergyQuantityQuantity().doubleValue());
        assertEquals("KWH", series.getFirst().getEnergyMeasurementUnitName());
        assertEquals("WTT", series.getLast().getEnergyMeasurementUnitName());
        assertEquals(1000.0, series.getLast().getSeriesPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuantity().doubleValue());
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
