package energy.eddie.tests.e2e;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

public class PlaywrightOptions implements OptionsFactory {

    @Override
    public Options getOptions() {
        return new Options()
                .setBaseUrl(System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:8080"))
                .setChannel("chromium");
    }
}
