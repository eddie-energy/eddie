// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationResult;
import energy.eddie.api.agnostic.data.needs.DataNeedNotSupportedResult;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ValidatedHistoricalDataStepConstraintTest {
    private static final SimulationContext DEFAULT_CTX = TestSimulationContext.create();
    private static final Measurement MSR = new Measurement(1.0, Measurement.MeasurementType.MEASURED);

    @Test
    void testConstraint_nonMatchingCalculationResult_returnsOk() {
        var ctx = ctx(new DataNeedNotSupportedResult("test"));
        var constraint = new ValidatedHistoricalDataStepConstraint(ctx);
        var step = step(ZonedDateTime.now(ZoneOffset.UTC));

        var res = constraint.violatesConstraint(step);

        assertInstanceOf(ConstraintOk.class, res);
    }

    @Test
    void testConstraint_nonMatchingModel_returnsOk() {
        var constraint = new ValidatedHistoricalDataStepConstraint(DEFAULT_CTX);

        var res = constraint.violatesConstraint(
                new StatusChangeStep(PermissionProcessStatus.CREATED, 0));

        assertInstanceOf(ConstraintOk.class, res);
    }

    @Test
    void testConstraint_startBeforeTimeframe_returnsViolation() {
        var ctx = ctx(new Timeframe(LocalDate.of(2026, Month.JANUARY, 15), LocalDate.of(2026, Month.JANUARY, 20)));
        var constraint = new ValidatedHistoricalDataStepConstraint(ctx);
        var step = step(LocalDate.of(2026, Month.JANUARY, 10).atStartOfDay(ZoneOffset.UTC));

        var res = constraint.violatesConstraint(step);

        var constraintViolation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals("Start of step is not within the energy timeframe", constraintViolation.message());
    }

    @Test
    void testConstraint_endAfterTimeframe_returnsViolation() {
        var ctx = ctx(new Timeframe(LocalDate.of(2026, Month.JANUARY, 15), LocalDate.of(2026, Month.JANUARY, 20)));
        var constraint = new ValidatedHistoricalDataStepConstraint(ctx);
        var step = new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData("mp",
                                                     LocalDate.of(2026, Month.JANUARY, 20)
                                                              .atTime(23, 0)
                                                              .atZone(ZoneOffset.UTC),
                                                     Duration.ofHours(1), List.of(MSR, MSR)));

        var res = constraint.violatesConstraint(step);

        var constraintViolation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals("End of step is not within the energy timeframe", constraintViolation.message());
    }

    @Test
    void testConstraint_validStep_returnsOk() {
        var constraint = new ValidatedHistoricalDataStepConstraint(DEFAULT_CTX);
        var step = step(ZonedDateTime.now(ZoneOffset.UTC));

        var res = constraint.violatesConstraint(step);

        assertInstanceOf(ConstraintOk.class, res);
    }

    @Test
    void testConstraint_validStepNoStartTime_returnsOk() {
        var constraint = new ValidatedHistoricalDataStepConstraint(DEFAULT_CTX);
        var step = new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData("mp", Optional.empty(), Duration.ofHours(1), List.of()));

        var res = constraint.violatesConstraint(step);

        assertInstanceOf(ConstraintOk.class, res);
    }

    private static SimulationContext ctx(Timeframe timeframe) {
        return ctx(new ValidatedHistoricalDataDataNeedResult(
                List.of(), null, timeframe, new ValidatedHistoricalDataDataNeed()));
    }

    private static SimulationContext ctx(DataNeedCalculationResult result) {
        return new SimulationContext(
                new DocumentStreams(new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME, "EP-ID"), new ObjectMapper()),
                "pid", "cid", "dnid", ZonedDateTime.now(ZoneOffset.UTC), result);
    }

    private static ValidatedHistoricalDataStep step(ZonedDateTime start) {
        return new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData("mp", start, Duration.ofHours(1), List.of(MSR)));
    }
}
