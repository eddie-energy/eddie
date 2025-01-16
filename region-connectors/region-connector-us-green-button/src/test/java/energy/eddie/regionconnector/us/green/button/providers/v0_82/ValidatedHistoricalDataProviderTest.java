package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;
import reactor.test.StepVerifier;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataProviderTest {
    @Spy
    private final PublishService publishService = new PublishService();
    @Spy
    private final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    @SuppressWarnings("unused")
    @Spy
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "client-id"
    );
    @SuppressWarnings("unused")
    @Spy
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "http://localhost",
            Map.of("company", "client-id"),
            Map.of("company", "client-secret"),
            Map.of("company", "token"),
            "http://localhost",
            "secret");
    @InjectMocks
    private ValidatedHistoricalDataProvider provider;

    @Test
    void testReceivesIdentifiablePayload_emitsValidatedHistoricalDataMarketDocument() throws FeedException {
        // Given
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

        // When
        publishService.publish(new IdentifiableSyndFeed(permissionRequest, feed));

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(publishService::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}