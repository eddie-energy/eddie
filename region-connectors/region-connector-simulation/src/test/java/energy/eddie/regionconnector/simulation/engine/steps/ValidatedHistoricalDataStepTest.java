// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.ValidatedHistoricalDataEmissionStep;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidatedHistoricalDataStepTest {

    @Test
    void testExecute_returnsEmissionStep() {
        // Given
        var reading = new SimulatedValidatedHistoricalData(
                "mid",
                ZonedDateTime.now(ZoneOffset.UTC),
                "PT15M",
                List.of()
        );
        var expected = List.of(new ValidatedHistoricalDataEmissionStep(reading));
        var step = new ValidatedHistoricalDataStep(reading);
        var ctx = TestSimulationContext.create();

        // When
        var res = step.execute(ctx);

        // Then
        assertEquals(expected, res);
    }
}