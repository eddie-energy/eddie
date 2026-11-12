// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v1_12;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;
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

    @BeforeEach
    void setUp() {
        marshaller.setPackagesToScan("org.naesb.espi");
    }

    @Test
    void testReceivesIdentifiablePayload_emitsValidatedHistoricalDataMarketDocument() throws FeedException {
        // Given
        var provider = new ValidatedHistoricalDataProvider(publishService, marshaller, cimConfig, config);

        // When
        publishService.publishValidatedHistoricalData(feed());

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(publishService::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testReceivesIdentifiablePayload_emitsDocumentEquivalentToV104() throws Exception {
        // Given
        var v104Future = new energy.eddie.regionconnector.us.green.button.providers.v1_04.ValidatedHistoricalDataProvider(
                publishService, marshaller, cimConfig, config
        ).getValidatedHistoricalDataMarketDocumentsStream().collectList().toFuture();
        var v112Future = new ValidatedHistoricalDataProvider(publishService, marshaller, cimConfig, config)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();

        // When
        publishService.publishValidatedHistoricalData(feed());
        publishService.close();

        // Then
        var v104 = v104Future.get();
        var v112 = v112Future.get();
        assertNotNull(v104);
        assertNotNull(v112);
        assertEquals(1, v104.size());
        assertEquals(1, v112.size());
        // The creation time is taken from the clock at mapping time, so it differs between versions
        assertEquals(List.of(),
                     ValidatedHistoricalDataVersionComparator.differencesIgnoring(v104.getFirst(),
                                                                                  v112.getFirst(),
                                                                                  "createdDateTime"));
        var envelope = v112.getFirst();
        assertEquals("US", envelope.getMessageDocumentHeader().getMetaInformation().getRegionCountry());
        var xml = new XmlMessageSerde().serialize(envelope);
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                   "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
    }

    private static IdentifiableSyndFeed feed() throws FeedException {
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var permissionRequest = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setCountryCode("US")
                .setCompanyId("company")
                .build();
        return new IdentifiableSyndFeed(permissionRequest, feed);
    }
}
