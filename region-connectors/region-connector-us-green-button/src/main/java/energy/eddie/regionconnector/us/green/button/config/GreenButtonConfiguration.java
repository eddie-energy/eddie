package energy.eddie.regionconnector.us.green.button.config;

import java.util.Map;

public interface GreenButtonConfiguration {
    String PREFIX = "region-connector.us.green.button.";
    String GREEN_BUTTON_CLIENT_API_TOKEN_KEY = PREFIX + "client.api.token";
    String GREEN_BUTTON_BASE_PATH_KEY = PREFIX + "basepath";
    String GREEN_BUTTON_CLIENT_IDS_KEY = PREFIX + "client.ids";
    String GREEN_BUTTON_CLIENT_SECRETS_KEY = PREFIX + "client.secrets";
    String GREEN_BUTTON_REDIRECT_URL_KEY = PREFIX + "redirect.url";

    String apiToken();

    String basePath();

    Map<String, String> clientIds();

    Map<String, String> clientSecrets();

    String redirectUrl();
}
