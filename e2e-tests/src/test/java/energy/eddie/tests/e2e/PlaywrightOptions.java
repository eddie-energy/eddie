package energy.eddie.tests.e2e;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

public class PlaywrightOptions implements OptionsFactory {

    public static final String BASE_URL = System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:8080");
    public static final boolean HEADLESS = !System.getenv().getOrDefault("E2E_HEADLESS", "true").equals("false");

    @Override
    public Options getOptions() {
        return new Options()
                .setBaseUrl(BASE_URL)
                .setChannel("chromium")
                .setHeadless(HEADLESS);
    }
}
