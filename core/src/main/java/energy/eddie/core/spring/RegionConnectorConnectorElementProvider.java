package energy.eddie.core.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
public class RegionConnectorConnectorElementProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConnectorConnectorElementProvider.class);
    private static final String CE_FILE_NAME = "ce.js";
    private final String ceElementContent;

    /**
     * {@link RestController} that serves the connector element javascript file.
     * It requires a bean named {@link RegionConnectorBeanPostProcessor#REGION_CONNECTOR_NAME_BEAN_NAME} and determines
     * the path of the connector element using this name.
     * <p>
     * The connector element is expected to be at the classpath under the following path: /public/{RC-COMMON-PATH}/{regionConnectorName}/ce.js
     * </p>
     * <p>
     * Whereas RC-COMMON-PATH is {@value RegionConnectorRegistrationBeanPostProcessor#ALL_REGION_CONNECTORS_BASE_URL_PATH} and
     * {@code regionConnectorName} is the parameter.
     * </p>
     *
     * @param regionConnectorName String bean containing the name of the region connector.
     * @throws FileNotFoundException Thrown if the ce.js file cannot be found on the classpath.
     */
    public RegionConnectorConnectorElementProvider(
            // Warning suppressed because the bean is programmatically registered
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") @Qualifier(RegionConnectorBeanPostProcessor.REGION_CONNECTOR_NAME_BEAN_NAME) String regionConnectorName
    ) throws FileNotFoundException {
        LOGGER.info("Got bean with name {}", regionConnectorName);

        var cePath = "/public/%s/%s/%s".formatted(RegionConnectorRegistrationBeanPostProcessor.ALL_REGION_CONNECTORS_BASE_URL_PATH, regionConnectorName, CE_FILE_NAME);

        LOGGER.info("cePath is {}", cePath);
        try {
            // fail early if connector element file cannot be found
            ceElementContent = readContentFromClasspath(cePath);
        } catch (IOException e) {
            throw new FileNotFoundException("Error while reading connector element javascript file for region connector \"%s\". Expected classpath for file \"%s\"%n%s".formatted(regionConnectorName, cePath, e.getMessage()));
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
