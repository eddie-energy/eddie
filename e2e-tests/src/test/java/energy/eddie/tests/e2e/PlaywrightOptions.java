package energy.eddie.tests.e2e;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

public class PlaywrightOptions implements OptionsFactory {

    public static final String E2E_BASE_URL = System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:8080");

    @Override
    public Options getOptions() {
        return new Options()
                .setBaseUrl(E2E_BASE_URL)
                .setChannel("chromium");
    }
}
