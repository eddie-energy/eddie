package energy.eddie.aiida.datasources.simulation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SimulationDataSourceTest {
    private SimulationDataSource simulator;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        var fixedInstant = Instant.parse("2023-09-11T22:00:00.00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
    }

    @Test
    void verify_bundleOfFourValuesIsGeneratedPerPeriod_andCloseEmitsCompleteOnFlux() {
        Duration period = Duration.ofSeconds(1);

        simulator = new SimulationDataSource("Test Simulator", fixedClock, period);

        StepVerifier.withVirtualTime(() -> simulator.start())
                .expectSubscription()
                .thenAwait(period)
                .expectNextCount(4)
                .thenAwait(period)
                .expectNextCount(4)
                .then(simulator::close)
                .expectComplete()
                .verify(Duration.ofSeconds(1));

        assertEquals("Test Simulator", simulator.name());
    }


    /**
     * Tests that the complete signal is emitted immediately when {@code close()} is called and not just
     * when the next value is emitted.
     */
    @Test
    void verify_close_immediatelyEmitsCompleteOnFlux() {
        simulator = new SimulationDataSource("Test Simulator", fixedClock, Duration.ofSeconds(200));

        var stepVerifier = StepVerifier.create(simulator.start())
                .expectComplete()
                .log()
                .verifyLater();

        simulator.close();

        stepVerifier.verify(Duration.ofSeconds(1));
    }
}