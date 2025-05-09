package energy.eddie.aiida.services.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import energy.eddie.aiida.models.monitoring.openslo.slo.SloDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;


@Service
public class OpenSloParserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSloParserService.class);

    @Scheduled(fixedRate = 5000)
    void parseSlo() throws IOException {
        LOGGER.debug("Parsing SLO data...");

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        var input = parseOpenSloDocument("aiida-ec-service-response-time-slo.yaml");
        var slo = objectMapper.readValue(input, SloDocument.class);
        LOGGER.debug("Parsed SLO: " + slo);
    }

    InputStream parseOpenSloDocument(String filePath) {
        String classpathFilePath = "openslo/" + filePath;

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classpathFilePath);
        if (inputStream == null) {
            LOGGER.error("Failed to load OpenSLO file: " + classpathFilePath);
            throw new RuntimeException("Failed to load OpenSLO file: " + classpathFilePath);
        }

        return inputStream;
    }
}
