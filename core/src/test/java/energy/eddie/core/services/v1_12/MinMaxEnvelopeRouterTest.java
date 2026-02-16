// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.v1_12;

import energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector;
import energy.eddie.api.v1_12.outbound.RegionConnectorMinMaxEnvelopeService;
import energy.eddie.cim.v1_12.recmmoe.MessageDocumentHeader;
import energy.eddie.cim.v1_12.recmmoe.MetaInformation;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.publisher.TestPublisher;

import static org.mockito.Mockito.*;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class MinMaxEnvelopeRouterTest {
    private static final String REGION_CONNECTOR_1 = "rc-1";
    private static final String REGION_CONNECTOR_2 = "rc-2";

    @Mock
    private RegionConnectorMinMaxEnvelopeService minMaxEnvelopeService1;
    @Mock
    private RegionConnectorMinMaxEnvelopeService minMaxEnvelopeService2;

    @Test
    void registerMinMaxEnvelopeConnector_routesEnvelopesToRegisteredServices() {
        // Given
        var publisher = TestPublisher.<RECMMOEEnvelope>create();
        var router = new MinMaxEnvelopeRouter();
        router.registerMinMaxEnvelopeService(REGION_CONNECTOR_1, minMaxEnvelopeService1);
        router.registerMinMaxEnvelopeService(REGION_CONNECTOR_2, minMaxEnvelopeService2);
        router.registerMinMaxEnvelopeConnector(new PlainMinMaxEnvelopeOutboundConnector(publisher.flux()));

        var envelope1 = envelopeFor(REGION_CONNECTOR_1);
        var envelope2 = envelopeFor(REGION_CONNECTOR_2);
        var envelopeUnknown = envelopeFor("unknown-rc");

        // When
        publisher.emit(envelope1, envelope2, envelopeUnknown);

        // Then
        verify(minMaxEnvelopeService1, times(1)).minMaxEnvelopeArrived(envelope1);
        verify(minMaxEnvelopeService2, times(1)).minMaxEnvelopeArrived(envelope2);
    }

    @Test
    void registerMinMaxEnvelopeService_withSameId_overridesPreviousService() {
        // Given
        var publisher = TestPublisher.<RECMMOEEnvelope>create();
        var router = new MinMaxEnvelopeRouter();
        router.registerMinMaxEnvelopeService(REGION_CONNECTOR_1, minMaxEnvelopeService1);
        router.registerMinMaxEnvelopeService(REGION_CONNECTOR_1, minMaxEnvelopeService2);
        router.registerMinMaxEnvelopeConnector(new PlainMinMaxEnvelopeOutboundConnector(publisher.flux()));
        var envelope = envelopeFor(REGION_CONNECTOR_1);

        // When
        publisher.emit(envelope);

        // Then
        verify(minMaxEnvelopeService1, never()).minMaxEnvelopeArrived(any());
        verify(minMaxEnvelopeService2, times(1)).minMaxEnvelopeArrived(envelope);
    }

    @Test
    void close_disposesSubscriptions() throws Exception {
        // Given
        var publisher = TestPublisher.<RECMMOEEnvelope>create();
        var router = new MinMaxEnvelopeRouter();
        router.registerMinMaxEnvelopeService(REGION_CONNECTOR_1, minMaxEnvelopeService1);
        router.registerMinMaxEnvelopeConnector(new PlainMinMaxEnvelopeOutboundConnector(publisher.flux()));
        var envelope1 = envelopeFor(REGION_CONNECTOR_1);
        var envelope2 = envelopeFor(REGION_CONNECTOR_1);

        // When
        publisher.emit(envelope1);
        router.close();
        publisher.emit(envelope2);

        // Then
        verify(minMaxEnvelopeService1, times(1)).minMaxEnvelopeArrived(envelope1);
        verify(minMaxEnvelopeService1, never()).minMaxEnvelopeArrived(envelope2);
    }

    private static RECMMOEEnvelope envelopeFor(String regionConnectorId) {
        return new RECMMOEEnvelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeader()
                                .withMetaInformation(
                                        new MetaInformation()
                                                .withRegionConnector(regionConnectorId)));
    }

    private record PlainMinMaxEnvelopeOutboundConnector(
            Flux<RECMMOEEnvelope> flux
    ) implements MinMaxEnvelopeOutboundConnector {

        @Override
        public Flux<RECMMOEEnvelope> getMinMaxEnvelopes() {
            return flux;
        }
    }
}
