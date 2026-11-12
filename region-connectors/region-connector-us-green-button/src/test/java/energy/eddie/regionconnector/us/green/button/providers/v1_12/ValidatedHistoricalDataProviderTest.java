// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v1_12;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidatedHistoricalDataProviderTest {
    private final PublishService publishService = new PublishService();
    private final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "client-id"
    );
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "http://localhost",
            Map.of("company", "client-id"),
            Map.of("company", "client-secret"),
            Map.of("company", "token"),
            "http://localhost",
            "secret");

    @Test
    void mapsPopulatedRawFixtureWithoutLegacyProvider() throws FeedException {
        marshaller.setPackagesToScan("org.naesb.espi");
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var permissionRequest = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setCountryCode("US")
                .setCompanyId("company")
                .build();

        var providerV112 = new ValidatedHistoricalDataProvider(publishService, marshaller, cimConfig, config);

        StepVerifier.create(providerV112.getValidatedHistoricalDataV112Stream())
                    .then(() -> publishService.publishValidatedHistoricalData(new IdentifiableSyndFeed(permissionRequest, feed)))
                    .then(publishService::close)
                    .assertNext(ValidatedHistoricalDataProviderTest::assertPopulatedDocument)
                    .verifyComplete();
    }

    private static void assertPopulatedDocument(Envelope envelope) {
        assertEquals("pid", envelope.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
        assertEquals("dnid", envelope.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
        var document = envelope.getVHDMarketDocument();
        assertNotNull(document);
        assertFalse(document.getTimeSeries().isEmpty());
        var pointCount = 0;
        for (var series : document.getTimeSeries()) {
            assertNotNull(series.getDateAndOrTimeDateTime());
            assertFalse(series.getSeriesPeriods().isEmpty());
            for (var period : series.getSeriesPeriods()) {
                assertNotNull(period.getResolution());
                assertNotNull(period.getTimeInterval());
                assertFalse(period.getPoints().isEmpty());
                for (var point : period.getPoints()) {
                    assertTrue(point.getPosition() > 0);
                    assertTrue(point.getEnergyQuantityQuantity() != null || point.getEnergyQualityQuantityQuantity() != null);
                    pointCount++;
                }
            }
        }
        assertTrue(pointCount > 0);
    }
}
