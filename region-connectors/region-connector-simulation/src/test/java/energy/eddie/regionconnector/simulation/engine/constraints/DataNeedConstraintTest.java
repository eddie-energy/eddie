package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
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
    private DataNeedsService dataNeedsService;

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
                                                     ZonedDateTime.now(ZoneOffset.UTC),
                                                     "PT15M",
                                                     List.of())
        );
        var constraint = new DataNeedConstraint(dataNeedsService, ctx);
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.empty());

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
                                                     ZonedDateTime.now(ZoneOffset.UTC),
                                                     "PT15M",
                                                     List.of())
        );
        var constraint = new DataNeedConstraint(dataNeedsService, ctx);
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of((new AiidaDataNeed(Set.of()))));

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
                                                     ZonedDateTime.now(ZoneOffset.UTC),
                                                     "PT15M",
                                                     List.of())
        );
        var constraint = new DataNeedConstraint(dataNeedsService, ctx);
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of((new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ZERO, Period.ZERO, null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT5M,
                        Granularity.P1Y
                ))));

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        assertEquals(new ConstraintOk(), res);
    }
}