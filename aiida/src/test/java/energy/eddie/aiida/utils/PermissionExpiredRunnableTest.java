package energy.eddie.aiida.utils;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static energy.eddie.aiida.TestUtils.verifyErrorLogStartsWith;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class PermissionExpiredRunnableTest {
    private static final LogCaptor logCaptor = LogCaptor.forClass(PermissionExpiredRunnable.class);
    @Mock
    Sinks.One<String> mockSink;

    @AfterAll
    public static void afterAll() {
        logCaptor.close();
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

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

        verifyErrorLogStartsWith("Error while trying to emit expiration signal for permission ", logCaptor);
    }
}