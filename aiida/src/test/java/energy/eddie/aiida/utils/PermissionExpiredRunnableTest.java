package energy.eddie.aiida.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionExpiredRunnableTest {
    @Mock
    Sinks.One<String> mockSink;
    @Mock(name = "energy.eddie.aiida.utils.PermissionExpiredRunnable")
    Logger logger = LoggerFactory.getLogger(PermissionExpiredRunnable.class);

    @Test
    void verify_runPublishesOnMono() {
        String permissionId = "SomeStringThatIsAnId";

        Sinks.One<String> sink = Sinks.one();
        var runnable = new PermissionExpiredRunnable(permissionId, Instant.now(), sink);

        runnable.run();

        StepVerifier.create(sink.asMono())
                .expectNext(permissionId)
                .expectComplete()
                .verify(Duration.ofSeconds(1));
    }

    @Test
    void givenErrorFromSink_permissionExpiredRunnable_logsError() {
        String permissionId = "SomeStringThatIsAnId";

        doReturn(Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER).when(mockSink).tryEmitValue(permissionId);

        var runnable = new PermissionExpiredRunnable(permissionId, Instant.now(), mockSink);

        runnable.run();

        verify(logger).info(startsWith("ExpirePermissionRunnable running for permission "), eq(permissionId), anyLong());
        verify(logger).error(startsWith("Error while trying to emit expiration signal for permission {}. Error was: {}"), eq(permissionId), any(Sinks.EmitResult.class));
        verifyNoMoreInteractions(logger);
    }
}