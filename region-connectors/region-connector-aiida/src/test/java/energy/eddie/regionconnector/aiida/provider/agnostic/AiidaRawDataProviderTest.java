package energy.eddie.regionconnector.aiida.provider.agnostic;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.regionconnector.aiida.streams.IdentifiableStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaRawDataProviderTest {
    @Mock
    IdentifiableStreams streams;

    @Test
    void getRawDataStream_emitsUnderlyingFlux() {
        var msg1 = mock(RawDataMessage.class);
        var msg2 = mock(RawDataMessage.class);
        when(streams.rawDataMessageFlux()).thenReturn(Flux.just(msg1, msg2));

        var provider = new AiidaRawDataProvider(streams);

        StepVerifier.create(provider.getRawDataStream())
                    .expectNext(msg1, msg2)
                    .thenCancel()
                    .verify();

        verify(streams).rawDataMessageFlux();
        verifyNoMoreInteractions(streams);
    }
}
