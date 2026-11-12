// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.ObtainMethod;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDocumentTest {
    @Test
    void givenMeterReadings_whenValue_thenReturnValidatedHistoricalDataMarketDocument() throws IOException {
        var response = MeteringDataProvider.loadMeteringDataShort();
        var pr = new DatadisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setMeteringPointId("mid")
                .setGranularity(Granularity.PT1H)
                .build();
        var intermediateMeteringDataMono = IntermediateMeteringData.fromMeteringData(response).block();
        var intermediateVHD = new IntermediateValidatedHistoricalDocument(
                new IdentifiableMeteringData(pr, intermediateMeteringDataMono),
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "EP-ID"),
                new DatadisConfiguration("client-id", "secret", "https://localhost")
        );

        var res = intermediateVHD.value();

        assertHistoricalDataRoundTrips(res);
    }

    @Test
    void consumptionAndEnabledProductionKeepCupsGroupingGlobalPositionsAndRawQuality() {
        var date = LocalDate.of(2026, 1, 1);
        var raw = new IntermediateMeteringData(List.of(
                new MeteringData("first", date, "01:00", 1.25, ObtainMethod.REAL, 0.5),
                new MeteringData("second", date, "02:00", 2.5, ObtainMethod.ESTIMATED, 0.75),
                new MeteringData("first", date, "03:00", 3.75, ObtainMethod.UNKNOWN, 1.0)), date, date);
        var permission = new DatadisPermissionRequestBuilder().setPermissionId("pid").setConnectionId("cid")
                .setDataNeedId("dnid").setMeteringPointId("meter").setGranularity(Granularity.PT1H)
                .setProductionSupport(true).build();
        var config = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME, "ep");
        var datadis = new DatadisConfiguration("client-id", "secret", "https://localhost");
        var metadata = new HistoricalDataEmissionMetadata();
        var input = new IdentifiableMeteringData(permission, raw);

        var series = new IntermediateValidatedHistoricalDocument(input, config, datadis, metadata)
                .value().getVHDMarketDocument().getTimeSeries();

        assertEquals(4, series.size());
        assertEquals(4, series.stream().map(value -> value.getMRID()).distinct().count());
        var firstConsumption = series.stream().filter(value -> "A02".equals(value.getFlowDirectionDirection())
                && "first".equals(value.getRegisteredResourceMRID().getValue())).findFirst().orElseThrow();
        var firstProduction = series.stream().filter(value -> "A01".equals(value.getFlowDirectionDirection())
                && "first".equals(value.getRegisteredResourceMRID().getValue())).findFirst().orElseThrow();
        var consumed = firstConsumption.getSeriesPeriods().getFirst().getPoints();
        var produced = firstProduction.getSeriesPeriods().getFirst().getPoints();
        assertEquals(List.of(1, 3), consumed.stream().map(point -> point.getPosition()).toList());
        assertEquals(List.of(1.25, 3.75), consumed.stream().map(point -> point.getEnergyQuantityQuantity().doubleValue()).toList());
        assertEquals(List.of(0.5, 1.0), produced.stream().map(point -> point.getEnergyQuantityQuantity().doubleValue()).toList());
        assertEquals(List.of("A04", "A02"), consumed.stream().map(point -> point.getEnergyQuantityQuality()).toList());
        assertEquals(metadata.child("first").id("A02"), firstConsumption.getMRID());
        assertEquals(metadata.child("first").id("A01"), firstProduction.getMRID());
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
