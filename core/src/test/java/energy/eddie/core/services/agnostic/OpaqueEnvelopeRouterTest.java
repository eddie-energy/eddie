// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.agnostic;

import energy.eddie.api.agnostic.opaque.OpaqueEnvelope;
import energy.eddie.api.agnostic.opaque.RegionConnectorOpaqueEnvelopeService;
import energy.eddie.api.agnostic.outbound.OpaqueEnvelopeOutboundConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.publisher.TestPublisher;

import java.time.ZonedDateTime;

import static org.mockito.Mockito.*;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class OpaqueEnvelopeRouterTest {
    private static final String REGION_CONNECTOR_1 = "rc-1";
    private static final String REGION_CONNECTOR_2 = "rc-2";

    @Mock
    private RegionConnectorOpaqueEnvelopeService opaqueEnvelopeService1;
    @Mock
    private RegionConnectorOpaqueEnvelopeService opaqueEnvelopeService2;

    @Test
    void registerOpaqueEnvelopeConnector_routesEnvelopesToRegisteredServices() {
        // Given
        var publisher = TestPublisher.<OpaqueEnvelope>create();
        var router = new OpaqueEnvelopeRouter();
        router.registerOpaqueEnvelopeService(REGION_CONNECTOR_1, opaqueEnvelopeService1);
        router.registerOpaqueEnvelopeService(REGION_CONNECTOR_2, opaqueEnvelopeService2);
        router.registerOpaqueEnvelopeConnector(new PlainOpaqueEnvelopeOutboundConnector(publisher.flux()));

        var envelope1 = envelopeFor(REGION_CONNECTOR_1);
        var envelope2 = envelopeFor(REGION_CONNECTOR_2);
        var envelopeUnknown = envelopeFor("unknown-rc");

        // When
        publisher.emit(envelope1, envelope2, envelopeUnknown);

        // Then
        verify(opaqueEnvelopeService1, times(1)).opaqueEnvelopeArrived(envelope1);
        verify(opaqueEnvelopeService2, times(1)).opaqueEnvelopeArrived(envelope2);
    }

    @Test
    void registerOpaqueEnvelopeService_withSameId_overridesPreviousService() {
        // Given
        var publisher = TestPublisher.<OpaqueEnvelope>create();
        var router = new OpaqueEnvelopeRouter();
        router.registerOpaqueEnvelopeService(REGION_CONNECTOR_1, opaqueEnvelopeService1);
        router.registerOpaqueEnvelopeService(REGION_CONNECTOR_1, opaqueEnvelopeService2);
        router.registerOpaqueEnvelopeConnector(new PlainOpaqueEnvelopeOutboundConnector(publisher.flux()));
        var envelope = envelopeFor(REGION_CONNECTOR_1);

        // When
        publisher.emit(envelope);

        // Then
        verify(opaqueEnvelopeService1, never()).opaqueEnvelopeArrived(any());
        verify(opaqueEnvelopeService2, times(1)).opaqueEnvelopeArrived(envelope);
    }

    @Test
    void close_disposesSubscriptions() throws Exception {
        // Given
        var publisher = TestPublisher.<OpaqueEnvelope>create();
        var router = new OpaqueEnvelopeRouter();
        router.registerOpaqueEnvelopeService(REGION_CONNECTOR_1, opaqueEnvelopeService1);
        router.registerOpaqueEnvelopeConnector(new PlainOpaqueEnvelopeOutboundConnector(publisher.flux()));
        var envelope1 = envelopeFor(REGION_CONNECTOR_1);
        var envelope2 = envelopeFor(REGION_CONNECTOR_1);

        // When
        publisher.emit(envelope1);
        router.close();
        publisher.emit(envelope2);

        // Then
        verify(opaqueEnvelopeService1, times(1)).opaqueEnvelopeArrived(envelope1);
        verify(opaqueEnvelopeService1, never()).opaqueEnvelopeArrived(envelope2);
    }

    private static OpaqueEnvelope envelopeFor(String regionConnectorId) {
        var id = "test-id";
        return new OpaqueEnvelope(regionConnectorId, id, id, id, id, ZonedDateTime.now(), "test-payload");
    }

    private record PlainOpaqueEnvelopeOutboundConnector(
            Flux<OpaqueEnvelope> flux
    ) implements OpaqueEnvelopeOutboundConnector {

        @Override
        public Flux<OpaqueEnvelope> getOpaqueEnvelopes() {
            return flux;
        }
    }
}
