package energy.eddie.regionconnector.simulation.engine;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredefinedScenariosTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testLoadingPredefinedScenarios_withClasspathUri_returnsPredefinedScenarios() {
        // Given
        var uri = "classpath:/scenarios/*.json";
        var expected = Set.of(
                "External Termination Scenario",
                "Validated Historical Data Scenario",
                "Failed To Externally Terminate Scenario",
                "Unable To Send Scenario"
        );

        // When
        var res = new PredefinedScenarios(objectMapper, uri);

        // Then
        assertEquals(expected, res.scenarioNames());
    }

    @Test
    void testLoadingPredefinedScenarios_withFileUri_returnsPredefinedScenarios() {
        // Given
        var uri = "./src/test/resources/scenarios";

        // When
        var res = new PredefinedScenarios(objectMapper, uri);

        // Then
        assertEquals(Set.of("Validated Historical Data Scenario"), res.scenarioNames());
    }


    @Test
    void testLoadingPredefinedScenarios_withSingleFilePath_returnsPredefinedScenarios() {
        // Given
        var uri = "./src/test/resources/scenarios/validated-historical-data-scenario.json";

        // When
        var res = new PredefinedScenarios(objectMapper, uri);

        // Then
        assertEquals(Set.of("Validated Historical Data Scenario"), res.scenarioNames());
    }
}