// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.ScenarioMetadata;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.results.SimulationConstraintViolations;
import energy.eddie.regionconnector.simulation.engine.results.SimulationStarted;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationEngineTest {
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "EP-ID"
    );
    @Mock
    private DataNeedsService dataNeedsService;

    @Test
    void testRun_withValidatedHistoricalData_emitsValidatedHistoricalDataAndPermissionMarketDocuments() throws InterruptedException {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(new ValidatedHistoricalDataDataNeed(new RelativeDuration(Period.ZERO,
                                                                                                 Period.ZERO,
                                                                                                 null),
                                                                            EnergyType.ELECTRICITY,
                                                                            Granularity.PT5M,
                                                                            Granularity.P1Y)));
        var scenario = new Scenario(
                "Test Scenario",
                List.of(
                        new StatusChangeStep(PermissionProcessStatus.CREATED, 0),
                        new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0),
                        new StatusChangeStep(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                             0
                        ),
                        new StatusChangeStep(PermissionProcessStatus.ACCEPTED, 0),
                        new ValidatedHistoricalDataStep(
                                new SimulatedValidatedHistoricalData("mid",
                                                                     ZonedDateTime.now(ZoneOffset.UTC),
                                                                     Granularity.PT15M.name(),
                                                                     List.of(
                                                                             new Measurement(10.0,
                                                                                             Measurement.MeasurementType.MEASURED)
                                                                     ))
                        ),
                        new StatusChangeStep(PermissionProcessStatus.FULFILLED, 0)
                )
        );
        var metadata = new ScenarioMetadata("cid", "pid", "dnid");
        var streams = new DocumentStreams(cimConfig);
        var engine = new SimulationEngine(streams, dataNeedsService);

        // When
        var res = engine.run(scenario, metadata);

        // Then
        var ok = assertInstanceOf(SimulationStarted.class, res);
        ok.thread().join();
        StepVerifier.create(streams.getValidatedHistoricalDataMarketDocumentsStream())
                    .expectNextCount(1)
                    .then(streams::close)
                    .verifyComplete();
        StepVerifier.create(streams.getConnectionStatusMessageStream())
                    .assertNext(csm -> assertEquals(PermissionProcessStatus.CREATED, csm.status()))
                    .assertNext(csm -> assertEquals(PermissionProcessStatus.VALIDATED, csm.status()))
                    .assertNext(csm -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                    csm.status()))
                    .assertNext(csm -> assertEquals(PermissionProcessStatus.ACCEPTED, csm.status()))
                    .assertNext(csm -> assertEquals(PermissionProcessStatus.FULFILLED, csm.status()))
                    .verifyComplete();
        StepVerifier.create(streams.getPermissionMarketDocumentStream())
                    .expectNextCount(5)
                    .verifyComplete();
    }

    @Test
    void testRun_invalidScenario_returnsViolations() {
        // Given
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.empty());
        var scenario = new Scenario(
                "Test Scenario",
                List.of(
                        new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0),
                        new StatusChangeStep(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                             0
                        ),
                        new ValidatedHistoricalDataStep(
                                new SimulatedValidatedHistoricalData("mid",
                                                                     ZonedDateTime.now(ZoneOffset.UTC),
                                                                     Granularity.PT15M.name(),
                                                                     List.of(
                                                                             new Measurement(10.0,
                                                                                             Measurement.MeasurementType.MEASURED)
                                                                     ))
                        )
                )
        );
        var metadata = new ScenarioMetadata("cid", "pid", "dnid");
        var streams = new DocumentStreams(cimConfig);
        var engine = new SimulationEngine(streams, dataNeedsService);

        // When
        var res = engine.run(scenario, metadata);

        // Then
        var violations = assertInstanceOf(SimulationConstraintViolations.class, res);
        assertEquals(3, violations.violations().size());
    }
}