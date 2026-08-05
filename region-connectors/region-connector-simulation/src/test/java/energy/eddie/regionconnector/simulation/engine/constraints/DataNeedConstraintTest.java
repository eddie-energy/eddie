// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedConstraintTest {
    @Mock
    private DataNeedCalculationService dataNeedsService;

    @Test
    void testConstraint_withStatusChangeStep_returnsOk() {
        // Given
        var step = new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0);
        var ctx = TestSimulationContext.create();
        var constraint = new DataNeedConstraint(dataNeedsService, ctx);

        // When
        var res = constraint.violatesConstraint(step);
        // Then
        assertEquals(new ConstraintOk(), res);
    }

    @Test
    void testConstraint_withUnknownDataNeedId_returnsViolation() {
        // Given
        var ctx = TestSimulationContext.create();
        var step = new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData("mid",
                                                     Optional.of(ZonedDateTime.now(ZoneOffset.UTC)),
                                                     Duration.ofMinutes(15),
                                                     List.of())
        );
        var constraint = new DataNeedConstraint(dataNeedsService, ctx);
        when(dataNeedsService.calculate("dnid", ctx.creationDateTime())).thenReturn(new DataNeedNotFoundResult());

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals("Data need dnid does not exist", violation.message());
    }

    @Test
    void testConstraint_withInvalidDataNeed_returnsViolation() {
        // Given
        var ctx = TestSimulationContext.create();
        var step = new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData("mid",
                                                     Optional.of(ZonedDateTime.now(ZoneOffset.UTC)),
                                                     Duration.ofMinutes(15),
                                                     List.of())
        );
        var constraint = new DataNeedConstraint(dataNeedsService, ctx);
        when(dataNeedsService.calculate("dnid", ctx.creationDateTime()))
                .thenReturn(new AiidaDataNeedResult(Set.of(), Set.of(), null, new InboundAiidaDataNeed()));

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals(
                "Data need dnid is not of type ValidatedHistoricalDataDataNeed, required by ValidatedHistoricalDataStep",
                violation.message()
        );
    }

    @Test
    void testConstraint_withValidDataNeed_returnsOk() {
        // Given
        var ctx = TestSimulationContext.create();
        var step = new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData("mid",
                                                     Optional.of(ZonedDateTime.now(ZoneOffset.UTC)),
                                                     Duration.ofMinutes(15),
                                                     List.of())
        );
        var constraint = new DataNeedConstraint(dataNeedsService, ctx);
        when(dataNeedsService.calculate("dnid", ctx.creationDateTime()))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeedResult(List.of(), null, null,
                                                                  new ValidatedHistoricalDataDataNeed(
                                                                          new RelativeDuration(Period.ZERO,
                                                                                               Period.ZERO,
                                                                                               null),
                                                                          EnergyType.ELECTRICITY,
                                                                          Granularity.PT5M,
                                                                          Granularity.P1Y
                                                                  )));

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        assertEquals(new ConstraintOk(), res);
    }
}