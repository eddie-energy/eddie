// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.v1_04;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaNearRealTimeDataMarketDocumentProviderTest {
    @Mock
    IdentifiableStreams streams;

    @Test
    void getRawDataStream_emitsUnderlyingFlux() {
        var msg1 = mock(RTDEnvelope.class);
        var msg2 = mock(RTDEnvelope.class);
        when(streams.nearRealTimeDataFlux()).thenReturn(Flux.just(msg1, msg2));

        var provider = new AiidaNearRealTimeDataMarketDocumentProvider(streams);

        StepVerifier.create(provider.getNearRealTimeDataMarketDocumentsStream())
                    .expectNext(msg1, msg2)
                    .thenCancel()
                    .verify();

        verify(streams).nearRealTimeDataFlux();
        verifyNoMoreInteractions(streams);
    }
}
