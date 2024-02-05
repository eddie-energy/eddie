package energy.eddie.tests.e2e;

public final class TestConfig {
    public static final String OUT_DIR_BASE_PATH = "build/playwright-results/";
    // Self-hosted runner is configured to run in the same network as the started containers, so we can directly access them
    public static final String EXAMPLE_APP_URL = "http://eddie-example-app:8081/prototype/main";
}
