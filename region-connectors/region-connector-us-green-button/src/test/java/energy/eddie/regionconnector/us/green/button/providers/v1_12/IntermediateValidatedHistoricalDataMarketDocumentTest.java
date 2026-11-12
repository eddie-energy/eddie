// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v1_12;

import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.providers.UnsupportedUnitException;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "id"
    );
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "http://localhost",
            Map.of("company", "client-id"),
            Map.of("company", "client-secret"),
            Map.of("company", "token"),
            "http://localhost",
            "secret"
    );

    @Test
    @SuppressWarnings("java:S5961")
    void testToVhd_returnsValidatedHistoricalDataMarketDocument() throws UnsupportedUnitException, FeedException, IOException {
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setCountryCode("US")
                .setCompanyId("company")
                .build();
        var intermediateVhd = new IntermediateValidatedHistoricalDataMarketDocument(
                new IdentifiableSyndFeed(pr, feed),
                marshaller,
                cimConfig,
                config
        );

        var res = intermediateVhd.toVhd();

        assertHistoricalDataRoundTrips(res.getFirst());

    }

    @Test
    void skipsMissingAndMalformedLinkedEntriesAndDistinguishesDuplicateRawEntries() throws FeedException, UnsupportedUnitException {
        var feed = new SyndFeedInput().build(new InputSource(XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml")));
        var entries = new ArrayList<>(feed.getEntries());
        var originalReading = entries.stream().filter(entry -> "ReadingType".equals(entry.getTitle())).findFirst().orElseThrow();
        var missing = new SyndEntryImpl();
        missing.setTitle("ReadingType");
        var malformed = new SyndEntryImpl();
        malformed.setTitle("ReadingType");
        malformed.setLinks(originalReading.getLinks());
        entries.addFirst(malformed);
        entries.addFirst(missing);
        entries.addLast(originalReading);
        feed.setEntries(entries);
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var permission = new GreenButtonPermissionRequestBuilder().setPermissionId("pid").setConnectionId("cid")
                .setDataNeedId("dnid").setCountryCode("US").setCompanyId("company").build();
        var metadata = new HistoricalDataEmissionMetadata();
        var input = new IdentifiableSyndFeed(permission, feed);

        var first = new IntermediateValidatedHistoricalDataMarketDocument(input, marshaller, cimConfig, config, metadata).toVhd();
        var second = new IntermediateValidatedHistoricalDataMarketDocument(input, marshaller, cimConfig, config, metadata).toVhd();

        assertEquals(2, first.size());
        assertEquals(2, second.size());
        assertNotEquals(first.getFirst().getVHDMarketDocument().getMRID(), first.getLast().getVHDMarketDocument().getMRID());
        assertEquals(first.getFirst().getVHDMarketDocument().getMRID(), second.getFirst().getVHDMarketDocument().getMRID());
        assertEquals(first.getLast().getVHDMarketDocument().getMRID(), second.getLast().getVHDMarketDocument().getMRID());
        assertEquals(first.getFirst().getVHDMarketDocument().getTimeSeries().getFirst().getSeriesPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuantity(),
                     first.getLast().getVHDMarketDocument().getTimeSeries().getFirst().getSeriesPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuantity());
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
