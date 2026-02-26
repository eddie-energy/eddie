// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.provider.v1_12;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaAcknowledgementMarketDocumentProviderTest {
    @Mock
    private IdentifiableStreams streams;

    @Test
    void getAcknowledgementDataMarketDocumentsStream_emitsUnderlyingFlux() {
        var msg1 = new AcknowledgementEnvelope();
        var msg2 = new AcknowledgementEnvelope();
        when(streams.acknowledgementCimFlux()).thenReturn(Flux.just(msg1, msg2));

        var provider = new AiidaAcknowledgementMarketDocumentProvider(streams);

        StepVerifier.create(provider.getAcknowledgementDataMarketDocumentsStream())
                    .expectNext(msg1, msg2)
                    .thenCancel()
                    .verify();

        verify(streams).acknowledgementCimFlux();
        verifyNoMoreInteractions(streams);
    }
}
