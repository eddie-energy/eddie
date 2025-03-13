package energy.eddie.aiida.datasources.simulation;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
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
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final SimulationDataSource DATA_SOURCE = new SimulationDataSource(
            new DataSourceDto(DATA_SOURCE_ID,
                              DataSourceType.Identifiers.SIMULATION,
                              AiidaAsset.SUBMETER.asset(),
                              "simulation",
                              true,
                              null,
                              1,
                              null),
            USER_ID
    );
    private SimulationAdapter simulator;

    @Test
    void testConstructorWithoutNameParameter() {
        // given, when
        simulator = new SimulationAdapter(DATA_SOURCE);

        // then
        assertEquals("simulation", simulator.dataSource().name());
        assertEquals(DATA_SOURCE_ID, simulator.dataSource().id());
    }

    @Test
    void verify_bundleOfFourValuesIsGeneratedPerPeriod_andCloseEmitsCompleteOnFlux() {
        simulator = new SimulationAdapter(DATA_SOURCE);
        var period = Duration.ofSeconds(DATA_SOURCE.simulationPeriod());

        StepVerifier.withVirtualTime(() -> simulator.start())
                    .expectSubscription()
                    .thenAwait(period)
                    .expectNextCount(1)
                    .thenAwait(period)
                    .expectNextCount(1)
                    .then(simulator::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

        assertEquals("simulation", simulator.dataSource().name());
        assertEquals(DATA_SOURCE_ID, simulator.dataSource().id());
    }


    /**
     * Tests that the complete signal is emitted immediately when {@code close()} is called and not just when the next
     * value is emitted.
     */
    @Test
    void verify_close_immediatelyEmitsCompleteOnFlux() {
        simulator = new SimulationAdapter(DATA_SOURCE);

        var stepVerifier = StepVerifier.create(simulator.start()).expectComplete().log().verifyLater();

        simulator.close();

        stepVerifier.verify(Duration.ofSeconds(1));
    }

    @Test
    void testHealth() {
        simulator = new SimulationAdapter(DATA_SOURCE);
        assertEquals(Status.UP, simulator.health().getStatus());
    }
}