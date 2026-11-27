// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.simulation;

import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SimulationDataSourceTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    @Mock
    private SimulationDataSource dataSource;
    private static final Integer POLLING_INTERVAL = 1;
    private SimulationAdapter simulator;

    @BeforeEach
    void setup() {
        lenient().when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        lenient().when(dataSource.userId()).thenReturn(USER_ID);
        lenient().when(dataSource.name()).thenReturn("simulation");
        lenient().when(dataSource.pollingInterval()).thenReturn(POLLING_INTERVAL);
    }

    @Test
    void testConstructorWithoutNameParameter() {
        // given, when
        simulator = new SimulationAdapter(dataSource);

        // then
        assertEquals("simulation", simulator.dataSource().name());
        assertEquals(DATA_SOURCE_ID, simulator.dataSource().id());
    }

    @Test
    void verify_bundleOfFourValuesIsGeneratedPerPeriod_andCloseEmitsCompleteOnFlux() {
        simulator = new SimulationAdapter(dataSource);
        var period = Duration.ofSeconds(POLLING_INTERVAL);

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
        simulator = new SimulationAdapter(dataSource);

        var stepVerifier = StepVerifier.create(simulator.start()).expectComplete().log().verifyLater();

        simulator.close();

        stepVerifier.verify(Duration.ofSeconds(1));
    }
}