package energy.eddie.regionconnector.simulation.web;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.ScenarioMetadata;
import energy.eddie.regionconnector.simulation.dtos.ScenarioRunConfiguration;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.PredefinedScenarios;
import energy.eddie.regionconnector.simulation.engine.SimulationEngine;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.results.SimulationConstraintViolations;
import energy.eddie.regionconnector.simulation.engine.results.SimulationStarted;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ScenarioController.class}, properties = {"cim.eligible-party.national-coding-scheme=NAT"})
@Import(PredefinedScenarios.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
@SuppressWarnings("unused")
class ScenarioControllerTest {
    @MockitoBean
    private DataNeedsService dataNeedsService;
    @MockitoBean
    private SimulationEngine engine;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MethodValidationPostProcessor methodValidationPostProcessor;

    @Test
    void testScenarios_returnsOk() throws Exception {
        // Given
        when(engine.run(any(), any()))
                .thenReturn(new SimulationStarted(Thread.startVirtualThread(() -> {})));
        var metadata = new ScenarioMetadata("cid", "pid", "dnid");

        // When
        mockMvc.perform(get("/scenarios"))
               // Then
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
               .andExpect(content().json("""
                                                 [
                                                   "External Termination Scenario",
                                                   "Failed To Externally Terminate Scenario",
                                                   "Validated Historical Data Scenario",
                                                   "Unable To Send Scenario"
                                                 ]
                                                 """));
    }

    @Test
    void testRunScenario_runsScenario() throws Exception {
        // Given
        when(engine.run(any(), any()))
                .thenReturn(new SimulationStarted(Thread.startVirtualThread(() -> {})));
        var scenario = new Scenario(
                "Test Scenario",
                List.of(
                        new StatusChangeStep(PermissionProcessStatus.CREATED),
                        new StatusChangeStep(PermissionProcessStatus.VALIDATED),
                        new StatusChangeStep(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR),
                        new StatusChangeStep(PermissionProcessStatus.ACCEPTED),
                        new ValidatedHistoricalDataStep(
                                new SimulatedValidatedHistoricalData("mid",
                                                                     ZonedDateTime.now(ZoneOffset.UTC),
                                                                     Granularity.PT15M.name(),
                                                                     List.of(
                                                                             new Measurement(10.0,
                                                                                             Measurement.MeasurementType.MEASURED)
                                                                     ))
                        ),
                        new StatusChangeStep(PermissionProcessStatus.FULFILLED)
                )
        );
        var scenarioConfig = new ScenarioRunConfiguration(
                scenario,
                new ScenarioMetadata("cid", "pid", "dnid")
        );
        var content = objectMapper.writeValueAsString(scenarioConfig);

        // When
        mockMvc.perform(post("/scenarios/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
               // Then
               .andExpect(status().isOk())
               .andExpect(content().json("{permissionId: \"pid\"}"));
    }

    @Test
    void testRunScenario_withInvalidScenario_returnsBadRequest() throws Exception {
        // Given
        when(engine.run(any(), any()))
                .thenReturn(new SimulationConstraintViolations(List.of(new ConstraintViolation("Invalid scenario"))));
        var scenario = new Scenario(
                "Test Scenario",
                List.of(
                        new StatusChangeStep(PermissionProcessStatus.VALIDATED)
                )
        );
        var scenarioConfig = new ScenarioRunConfiguration(
                scenario,
                new ScenarioMetadata("cid", "pid", "dnid")
        );
        var content = objectMapper.writeValueAsString(scenarioConfig);

        // When
        mockMvc.perform(post("/scenarios/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(content().json("{errors:[{message: \"Invalid scenario\"}]}"));
    }

    @Test
    void testRunPredefinedScenario_runsScenario() throws Exception {
        // Given
        when(engine.run(any(), any()))
                .thenReturn(new SimulationStarted(Thread.startVirtualThread(() -> {})));
        var metadata = new ScenarioMetadata("cid", "pid", "dnid");
        var content = objectMapper.writeValueAsString(metadata);

        // When
        mockMvc.perform(post("/scenarios/Validated Historical Data Scenario/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
               // Then
               .andExpect(status().isOk())
               .andExpect(content().json("{permissionId: \"pid\"}"));
    }

    @Test
    void testRunPredefinedScenario_withUnknownScenario_returnsNotFound() throws Exception {
        // Given
        when(engine.run(any(), any()))
                .thenReturn(new SimulationStarted(Thread.startVirtualThread(() -> {})));
        var metadata = new ScenarioMetadata("cid", "pid", "dnid");
        var content = objectMapper.writeValueAsString(metadata);

        // When
        mockMvc.perform(post("/scenarios/invalid-name/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
               // Then
               .andExpect(status().isNotFound());
    }
}