// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableECMPList;
import energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p10.EdaECMPList01p10InboundMessageFactory;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdaEnergySharingReferenceDataMarketDocumentProviderTest {
    @Mock
    private IdentifiableStreams streams;

    @Test
    void testGetEnergySharingReferenceDataMarketDocumentStream() {
        // Given
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("at.ebutilities.schemata");
        var factory = new EdaECMPList01p10InboundMessageFactory(marshaller);
        var ecmpList = factory.parseInputStream(getClass().getResourceAsStream("/xsd/ecmplist/_01p10/ecmplist.xml"));
        var pr = new SimplePermissionRequest("pid", "cid", "dnid");
        var id = new IdentifiableECMPList(ecmpList, pr);
        when(streams.ecmpListStream()).thenReturn(Flux.just(id));
        var provider = new EdaEnergySharingReferenceDataMarketDocumentProvider(streams);

        // When
        StepVerifier.create(provider.getEnergySharingReferenceDataMarketDocumentStream())
                    .expectNextCount(1)
                    .verifyComplete();
    }
}