package energy.eddie.regionconnector.us.green.button.config;

import java.util.Map;

public record PlainGreenButtonConfiguration(
        String apiToken,
        String basePath,
        Map<String, String> clientIds,
        Map<String, String> clientSecrets,
        String redirectUrl
) implements GreenButtonConfiguration {
}
