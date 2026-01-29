// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;
import reactor.test.StepVerifier;

class AccountingPointDataProviderTest {

    @Test
    void provider_publishesMappedData() throws FeedException {
        // Given
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var xml = XmlLoader.xmlStreamFromResource("/xml/retailcustomer/accounting_point_data.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var pr = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setCountryCode("US")
                .setCompanyId("company")
                .build();
        var publishService = new PublishService();
        var provider = new AccountingPointDataProvider(publishService, marshaller);

        // When
        publishService.publishAccountingPointData(new IdentifiableSyndFeed(pr, feed));

        // Then
        StepVerifier.create(provider.getAccountingPointEnvelopeFlux())
                    .then(publishService::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}