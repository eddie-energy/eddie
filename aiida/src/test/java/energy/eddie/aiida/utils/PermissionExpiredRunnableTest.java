package energy.eddie.aiida.utils;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

class PermissionExpiredRunnableTest {
    @Test
    void verify_runCallsListener() {
        String permissionId = "SomeStringThatIsAnId";

        Sinks.One<String> sink = Sinks.one();
        var runnable = new PermissionExpiredRunnable(permissionId, Instant.now(), sink);

        runnable.run();

        StepVerifier.create(sink.asMono())
                .expectNext(permissionId)
                .expectComplete()
                .verify(Duration.ofSeconds(1));
    }
}