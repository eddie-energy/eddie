package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.regionconnector.shared.utils.CommonPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static energy.eddie.regionconnector.shared.utils.CommonPaths.CE_FILE_NAME;
import static energy.eddie.regionconnector.shared.utils.CommonPaths.getClasspathForCeElement;

/**
 * The {@code RegionConnectorConnectorElementProvider} creates an HTTP GET mapping for the connector element javascript
 * file for each individual region connector.
 */
@RegionConnectorExtension
@RestController
public class RegionConnectorConnectorElementProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConnectorConnectorElementProvider.class);
    private final String ceElementContent;

    /**
     * {@link RestController} that serves the connector element javascript file. It requires a bean named
     * {@link RegionConnectorNameExtension#REGION_CONNECTOR_NAME_BEAN_NAME} and determines the path of the connector
     * element using this name.
     * <p>
     * The connector element is expected to be at the classpath under the following path:
     * /public/{RC-COMMON-PATH}/{regionConnectorName}/ce.js
     * </p>
     * <p>
     * Whereas RC-COMMON-PATH is {@value CommonPaths#ALL_REGION_CONNECTORS_BASE_URL_PATH} and
     * {@code regionConnectorName} is the parameter.
     * </p>
     *
     * @param regionConnectorName String bean containing the name of the region connector.
     * @throws FileNotFoundException Thrown if the ce.js file cannot be found on the classpath.
     */
    public RegionConnectorConnectorElementProvider(
            @Qualifier(RegionConnectorNameExtension.REGION_CONNECTOR_NAME_BEAN_NAME) String regionConnectorName
    ) throws FileNotFoundException {
        var cePath = getClasspathForCeElement(regionConnectorName);

        LOGGER.info("Registering new GET mapping for file classpath:{}", cePath);
        try {
            // fail early if connector element file cannot be found
            ceElementContent = readContentFromClasspath(cePath);
        } catch (IOException e) {
            throw new FileNotFoundException(
                    "Error while reading connector element javascript file for region connector \"%s\". Expected classpath for file \"%s\"%n%s".formatted(
                            regionConnectorName,
                            cePath,
                            e.getMessage()));
        }
    }

    @GetMapping(value = "/" + CE_FILE_NAME, produces = "text/javascript")
    public String javascriptConnectorElement() {
        return ceElementContent;
    }

    /**
     * Reads and returns the String content of the passed file on the classpath.
     *
     * @param path Classpath of file to read.
     * @return String representation of the file (UTF_8 encoded).
     * @throws IOException If an error occurs while reading the content.
     */
    private String readContentFromClasspath(String path) throws IOException {
        try (InputStream resourceInputStream = getClass().getResourceAsStream(path)) {
            if (resourceInputStream == null)
                throw new FileNotFoundException(path);
            return new String(resourceInputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
