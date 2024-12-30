package energy.eddie.regionconnector.simulation.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PredefinedScenarios {
    private static final List<String> SCENARIO_PATHS = List.of(
            "/scenarios/external-termination-scenario.json",
            "/scenarios/failed-to-externally-terminate-scenario.json",
            "/scenarios/unable-to-send-scenario.json",
            "/scenarios/validated-historical-data-scenario.json"
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(PredefinedScenarios.class);
    private final ObjectMapper objectMapper;
    private final Map<String, Scenario> scenarios;

    public PredefinedScenarios(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.scenarios = loadScenarios();
    }

    public Set<String> scenarioNames() {
        return scenarios.keySet();
    }

    public Scenario getScenario(String scenarioName) throws ScenarioNotFoundException {
        if (!scenarios.containsKey(scenarioName)) {
            throw new ScenarioNotFoundException(scenarioName);
        }
        return scenarios.get(scenarioName);
    }

    private Map<String, Scenario> loadScenarios() {
        Map<String, Scenario> map = HashMap.newHashMap(SCENARIO_PATHS.size());
        for (var path : SCENARIO_PATHS) {
            InputStream stream = getClass().getResourceAsStream(path);
            try {
                var scenario = objectMapper.readValue(stream, Scenario.class);
                map.put(scenario.name(), scenario);
            } catch (IOException e) {
                LOGGER.warn("Error loading scenario '{}'", path, e);
            }
        }
        return map;
    }

    public static class ScenarioNotFoundException extends Exception {
        public ScenarioNotFoundException(String scenarioName) {
            super("Scenario not found: " + scenarioName);
        }
    }
}
