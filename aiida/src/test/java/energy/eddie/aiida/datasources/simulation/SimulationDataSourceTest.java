package energy.eddie.aiida.datasources.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SimulationDataSourceTest {
    private static final UUID dataSourceId = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID userId = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private SimulationAdapter simulator;

    @Test
    void testConstructorWithoutNameParameter() {
        // given
        Duration period = Duration.ofSeconds(1);

        // when
        simulator = new SimulationAdapter(dataSourceId, userId, "simulation", period);

        // then
        assertEquals("simulation", simulator.name());
        assertEquals(dataSourceId, simulator.id());
    }

    @Test
    void verify_bundleOfFourValuesIsGeneratedPerPeriod_andCloseEmitsCompleteOnFlux() {
        Duration period = Duration.ofSeconds(1);

        simulator = new SimulationAdapter(dataSourceId, userId, "Test Simulator", period);

        StepVerifier.withVirtualTime(() -> simulator.start())
                    .expectSubscription()
                    .thenAwait(period)
                    .expectNextCount(1)
                    .thenAwait(period)
                    .expectNextCount(1)
                    .then(simulator::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        assertEquals("Test Simulator", simulator.name());
        assertEquals(dataSourceId, simulator.id());
    }


    /**
     * Tests that the complete signal is emitted immediately when {@code close()} is called and not just when the next
     * value is emitted.
     */
    @Test
    void verify_close_immediatelyEmitsCompleteOnFlux() {
        simulator = new SimulationAdapter(dataSourceId, userId, "Test Simulator", Duration.ofSeconds(200));

        var stepVerifier = StepVerifier.create(simulator.start()).expectComplete().log().verifyLater();

        simulator.close();

        stepVerifier.verify(Duration.ofSeconds(1));
    }

    @Test
    void testHealth() {
        simulator = new SimulationAdapter(dataSourceId, userId, "Test Simulator", Duration.ofSeconds(200));
        assertEquals(Status.UP, simulator.health().getStatus());
    }
}