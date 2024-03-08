package energy.eddie.core.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.Mockito.mock;

class PermissionServiceTest {

    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    private static Mvp1ConnectionStatusMessageProvider createProvider(Sinks.Many<ConnectionStatusMessage> sink) {
        return new Mvp1ConnectionStatusMessageProvider() {
            @Override
            public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
                return sink.asFlux();
            }

            @Override
            public void close() throws Exception {
                sink.tryEmitComplete();
            }
        };
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() {
        // Given
        PermissionService service = new PermissionService();
        Sinks.Many<ConnectionStatusMessage> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<ConnectionStatusMessage> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        Mvp1ConnectionStatusMessageProvider provider1 = createProvider(sink1);
        Mvp1ConnectionStatusMessageProvider provider2 = createProvider(sink2);

        // When
        var flux = service.getConnectionStatusMessageStream();
        StepVerifier.create(flux)
                .then(() -> {
                    service.registerProvider(provider1);

                    sink1.tryEmitNext(new ConnectionStatusMessage("one", "one", "one", mock(DataSourceInformation.class), PermissionProcessStatus.CREATED));
                    sink1.tryEmitNext(new ConnectionStatusMessage("three", "three", "three", mock(DataSourceInformation.class), PermissionProcessStatus.INVALID));
                })
                // Then
                .expectNextCount(2)
                // When
                .then(() -> {
                    service.registerProvider(provider2);

                    sink2.tryEmitNext(new ConnectionStatusMessage("two", "two", "two", mock(DataSourceInformation.class), PermissionProcessStatus.VALIDATED));
                    sink1.tryEmitNext(new ConnectionStatusMessage("four", "four", "four", mock(DataSourceInformation.class), PermissionProcessStatus.INVALID));
                })
                .expectNextCount(2)
                // Then
                .thenCancel()
                .verify();
    }
}
