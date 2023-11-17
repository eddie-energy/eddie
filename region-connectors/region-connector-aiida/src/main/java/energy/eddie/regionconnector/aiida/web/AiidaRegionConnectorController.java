package energy.eddie.regionconnector.aiida.web;

import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@RestController
public class AiidaRegionConnectorController {
    /**
     * We have to check two different paths depending if the Region-Connector is run by the core or in standalone.
     */
    private static final String[] CE_DEV_PATHS = new String[]{
            "./region-connectors/region-connector-aiida/src/main/resources/public/ce.js",
            "./src/main/resources/public/ce.js"
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnectorController.class);
    private final AiidaRegionConnectorService aiidaService;
    private final Environment environment;

    @Autowired
    public AiidaRegionConnectorController(Environment environment, AiidaRegionConnectorService aiidaService) {
        this.environment = environment;
        this.aiidaService = aiidaService;
    }

    private static String findCEDevPath() throws FileNotFoundException {
        for (String ceDevPath : CE_DEV_PATHS) {
            if (new File(ceDevPath).exists()) {
                return ceDevPath;
            }
        }
        throw new FileNotFoundException();
    }

    @GetMapping(value = "/ce.js", produces = "text/javascript")
    public String javascriptConnectorElement() throws IOException {
        try (InputStream in = getCEInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private InputStream getCEInputStream() throws FileNotFoundException {
        return !environment.matchesProfiles("dev")
                ? new FileInputStream(findCEDevPath())
                : Objects.requireNonNull(getClass().getResourceAsStream("/public/ce.js"));
    }
}
