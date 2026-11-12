// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_12;

import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesData;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesTransaction;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    @Test
    void toVhd_withValues_returnsVhdsWithTimeSeries() {
        var response = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .build();
        var intermediateVHD = new IntermediateValidatedHistoricalDataMarketDocument(List.of(response), pr);

        var res = intermediateVHD.toVhds().getFirst();

        assertHistoricalDataRoundTrips(res);
    }

    @Test
    void repeatedRawResponsesKeepSourceIdentifiersButGetDistinctCorrelatedSeriesIdentifiers() {
        var response = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var permission = new FingridPermissionRequestBuilder().setPermissionId("pid")
                .setConnectionId("cid").setDataNeedId("dnid").build();
        var metadata = new HistoricalDataEmissionMetadata();
        var source = List.of(response, response);

        var first = new IntermediateValidatedHistoricalDataMarketDocument(source, permission, metadata).toVhds();
        var second = new IntermediateValidatedHistoricalDataMarketDocument(source, permission, metadata).toVhds();

        assertEquals(2, first.size());
        assertEquals(response.data().header().identification(), first.getFirst().getVHDMarketDocument().getMRID());
        assertEquals(response.data().header().identification(), first.getLast().getVHDMarketDocument().getMRID());
        var firstSeries = first.getFirst().getVHDMarketDocument().getTimeSeries().getFirst();
        var lastSeries = first.getLast().getVHDMarketDocument().getTimeSeries().getFirst();
        assertNotEquals(firstSeries.getMRID(), lastSeries.getMRID());
        assertEquals(firstSeries.getMRID(), second.getFirst().getVHDMarketDocument().getTimeSeries().getFirst().getMRID());
        assertEquals(lastSeries.getMRID(), second.getLast().getVHDMarketDocument().getTimeSeries().getFirst().getMRID());
    }

    @Test
    void absentSeriesAndReasonsRemainOptionalWithoutInventingMeasurements() {
        var fixture = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var empty = new TimeSeriesResponse(new TimeSeriesData(fixture.data().header(),
                new TimeSeriesTransaction(null, null, null)));
        var permission = new FingridPermissionRequestBuilder().setPermissionId("pid")
                .setConnectionId("cid").setDataNeedId("dnid").build();

        var documents = new IntermediateValidatedHistoricalDataMarketDocument(List.of(empty), permission).toVhds();

        assertEquals(1, documents.size());
        var document = documents.getFirst().getVHDMarketDocument();
        assertEquals(fixture.data().header().creation(), document.getCreatedDateTime());
        assertEquals(1, document.getTimeSeries().size());
        var series = document.getTimeSeries().getFirst();
        assertTrue(series.getSeriesPeriods().isEmpty());
        assertNull(series.getEnergyMeasurementUnitName());
        assertNull(series.getAccountingPointMRID().getValue());
        assertNull(series.getReasonText());
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
