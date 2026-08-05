// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.DataNeedNotFoundResult;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationConstraintsTest {
    @Mock
    private DataNeedCalculationService dataNeedsService;

    @Test
    void testViolatesConstraints_returnsViolations() {
        // Given
        var ctx = TestSimulationContext.create();
        when(dataNeedsService.calculate("dnid", ctx.creationDateTime())).thenReturn(new DataNeedNotFoundResult());
        var scenario = new Scenario(
                "Test Scenario",
                List.of(
                        new StatusChangeStep(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                             0
                        ),
                        new ValidatedHistoricalDataStep(
                                new SimulatedValidatedHistoricalData("mid",
                                                                     Optional.of(ZonedDateTime.now(ZoneOffset.UTC)),
                                                                     Granularity.PT15M.duration(),
                                                                     List.of(
                                                                             new Measurement(10.0,
                                                                                             Measurement.MeasurementType.MEASURED)
                                                                     ))
                        )
                )
        );
        var constraints = new SimulationConstraints(scenario, ctx, dataNeedsService);

        // When
        var res = constraints.violatesConstraints();

        // Then
        assertEquals(3, res.size());
    }
}