package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class PredefinedScenarios {
    private static final Logger LOGGER = LoggerFactory.getLogger(PredefinedScenarios.class);
    private final ObjectMapper objectMapper;
    private final Map<String, Scenario> scenarios;

    public PredefinedScenarios(
            ObjectMapper objectMapper,
            @Value("${region-connector.sim.scenarios:classpath:/scenarios/*.json}") String path
    ) {
        this.objectMapper = objectMapper;
        this.scenarios = loadScenarios(path);
    }

    public Set<String> scenarioNames() {
        return scenarios.keySet();
    }

    public Optional<Scenario> getScenario(String scenarioName) {
        if (!scenarios.containsKey(scenarioName)) {
            return Optional.empty();
        }
        return Optional.of(scenarios.get(scenarioName));
    }

    private Map<String, Scenario> loadScenarios(String path) {
        try {
            var uri = URI.create(path);
            return "classpath".equals(uri.getScheme())
                    ? loadJsonFilesFromClasspath(uri)
                    : loadJsonFilesFromFileSystem(uri);
        } catch (IOException e) {
            LOGGER.warn("Error loading scenario '{}'", path, e);
            return Map.of();
        }
    }

    private Map<String, Scenario> loadJsonFilesFromClasspath(URI classpathLocation) throws IOException {
        var map = new HashMap<String, Scenario>();
        var resources = new PathMatchingResourcePatternResolver().getResources("classpath*:" + classpathLocation.getPath());
        for (var resource : resources) {
            if (!resource.exists()) {
                continue;
            }
            var src = resource.getContentAsString(StandardCharsets.UTF_8);
            var scenario = objectMapper.readValue(src, Scenario.class);
            map.put(scenario.name(), scenario);
        }
        return map;
    }

    private Map<String, Scenario> loadJsonFilesFromFileSystem(URI uri) throws IOException {
        var map = new HashMap<String, Scenario>();
        var path = Paths.get(uri.getPath());
        if (Files.isDirectory(path)) {
            try (var walker = Files.walk(path)) {
                walker.filter(PredefinedScenarios::isJsonFile)
                      .forEach(p -> {
                          try {
                              var scenario = objectMapper.readValue(p.toFile(), Scenario.class);
                              map.put(scenario.name(), scenario);
                          } catch (JacksonException e) {
                              LOGGER.warn("Couldn't parse scenario file {}", p, e);
                          }
                      });
            }
        } else if (isJsonFile(path)) {
            var scenario = objectMapper.readValue(path.toFile(), Scenario.class);
            map.put(scenario.name(), scenario);
        }
        return map;
    }

    private static boolean isJsonFile(Path path) {
        return path.toString().endsWith(".json");
    }
}
