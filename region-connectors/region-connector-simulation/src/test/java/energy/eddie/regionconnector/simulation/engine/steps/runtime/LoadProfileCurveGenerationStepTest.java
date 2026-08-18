// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.exceptions.ExecutionException;
import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.loadcurve.Profile;
import energy.eddie.regionconnector.simulation.engine.steps.loadcurve.StandardProfiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadProfileCurveGenerationStepTest {
    @Mock
    private StandardProfiles standardProfiles;

    @Test
    void testExecuteSuccessfully() throws ExecutionException {
        // Given
        double maxEnergy = 100.0;
        String meteringPoint = "MP1";
        when(standardProfiles.getProfile("mockProfile")).thenReturn(Optional.of(new Profile(List.of(BigDecimal.ONE))));
        var step = new LoadProfileCurveGenerationStep(
                Map.of(DayOfWeek.MONDAY, "mockProfile"),
                "mockProfile",
                maxEnergy,
                meteringPoint,
                standardProfiles
        );

        SimulationContext ctx = TestSimulationContext.create();

        // When
        var result = step.execute(ctx);

        // Then
        var vstep = assertInstanceOf(ValidatedHistoricalDataEmissionStep.class, result.getFirst());
        var measurements = Collections.nCopies(96, new Measurement(100.0, Measurement.MeasurementType.MEASURED));
        assertEquals(
                new ValidatedHistoricalDataEmissionStep(
                        new SimulatedValidatedHistoricalData(
                                "MP1",
                                LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC),
                                Duration.ofMinutes(15),
                                measurements
                        )
                ),
                vstep);
    }
}