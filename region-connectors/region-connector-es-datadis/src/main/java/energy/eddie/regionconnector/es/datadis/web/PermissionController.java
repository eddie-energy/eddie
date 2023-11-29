package energy.eddie.regionconnector.es.datadis.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.BASE_PATH;

@RestController
@RequestMapping(BASE_PATH)
public class PermissionController {
    private static final String CE_JS = "ce.js";
    private static final String[] CE_DEV_PATHS = new String[]{
            "./region-connectors/region-connector-es-datadis/src/main/resources/public" + BASE_PATH + CE_JS,
            "./src/main/resources/public" + BASE_PATH + CE_JS
    };
    private static final String CE_PRODUCTION_PATH = "/public" + BASE_PATH + CE_JS;
    // this path will stay hard-coded
//    TODO @SuppressWarnings("java:S1075")
    private static final String PERMISSION_STATUS_PATH = "/permission-status";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionController.class);
    private final Environment environment;

    public PermissionController(Environment environment) {
        this.environment = environment;
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
    public String javascriptConnectorElement() {
        try (InputStream in = getCEInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private InputStream getCEInputStream() throws FileNotFoundException {
        return !environment.matchesProfiles("dev")
                ? new FileInputStream(findCEDevPath())
                : Objects.requireNonNull(getClass().getResourceAsStream(CE_PRODUCTION_PATH));
    }
}
