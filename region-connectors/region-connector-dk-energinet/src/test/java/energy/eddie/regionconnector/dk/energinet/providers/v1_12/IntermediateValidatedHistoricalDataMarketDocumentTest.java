// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.dk.energinet.DtoLoader;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    @Test
    void toVhd_withValues_returnsVhdsWithTimeSeries() throws IOException {
        var response = DtoLoader.loadValidatedHistoricalData();
        var pr = getPermissionRequest();
        var intermediateVHD = new IntermediateValidatedHistoricalDataMarketDocument(
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "ep-id"),
                new IdentifiableApiResponse(pr, Objects.requireNonNull(response.getResult()).getFirst())
        );

        var res = intermediateVHD.value();

        assertHistoricalDataRoundTrips(res);
    }

    private static EnerginetPermissionRequest getPermissionRequest() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return new EnerginetPermissionRequest("pid",
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
    }

    @Test
    void missingProviderMetadataAndOptionalPeriodsUseIndependentCorrelatedFallbacks() throws IOException {
        var response = Objects.requireNonNull(DtoLoader.loadValidatedHistoricalData().getResult()).getFirst();
        var raw = response.getMyEnergyDataMarketDocument();
        raw.setmRID(null);
        raw.setCreatedDateTime(null);
        var rawSeries = raw.getTimeSeries().getFirst();
        rawSeries.setPeriod(null);
        var metadata = new HistoricalDataEmissionMetadata();
        var input = new IdentifiableApiResponse(getPermissionRequest(), response);
        var config = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "ep-id");

        var first = new IntermediateValidatedHistoricalDataMarketDocument(config, input, metadata).value();
        var second = new IntermediateValidatedHistoricalDataMarketDocument(config, input, metadata).value();

        assertEquals(metadata.id("document"), first.getVHDMarketDocument().getMRID());
        assertEquals(first.getVHDMarketDocument().getMRID(), second.getVHDMarketDocument().getMRID());
        assertEquals(metadata.envelopeCreated().toInstant(), first.getVHDMarketDocument().getCreatedDateTime().toInstant());
        assertEquals(1, first.getVHDMarketDocument().getTimeSeries().size());
        var series = first.getVHDMarketDocument().getTimeSeries().getFirst();
        assertEquals(rawSeries.getmRID(), series.getMRID());
        assertTrue(series.getSeriesPeriods().isEmpty());
        assertEquals(first.getVHDMarketDocument().getCreatedDateTime(), series.getDateAndOrTimeDateTime());
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
