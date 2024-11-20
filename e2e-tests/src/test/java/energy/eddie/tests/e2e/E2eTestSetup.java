package energy.eddie.tests.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.util.Objects;

import static energy.eddie.tests.e2e.TestConfig.EXAMPLE_APP_URL;

/**
 * This E2E base test class setups the playwright environment and automatically takes a screenshot of <code>page</code>
 * when an error occurs or at the end of the test. Initially, the example app main screen is loaded as
 * <code>page</code>.
 */
public class E2eTestSetup {
    protected static Playwright playwright;
    protected static Browser browser;
    private final String screenshotsDir = TestConfig.OUT_DIR_BASE_PATH + this.getClass().getSimpleName();
    protected Page page;

    protected void navigateToRegionConnector(String dataNeed, String country, String permissionAdministrator) {
        if (dataNeed != null) {
            page.locator("#data-need-select")
                .selectOption(new SelectOption().setLabel(dataNeed),
                              new Locator.SelectOptionOptions().setTimeout(1000));
        }

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).first().click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue")).click();

        if (country != null) {
            page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(country)).click();
        }

        if (permissionAdministrator != null) {
            page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Permission Administrator")).click();
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(permissionAdministrator)).click();
        }

        if (country != null) {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue")).click();
        }

        // Wait for RC element to load
        page.getByText("Follow the instructions for")
            .waitFor(new Locator.WaitForOptions().setTimeout(5000)); // 5 sec
    }

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        page = browser.newPage();
        page.setViewportSize(1920, 2080);
        var exampleApp = System.getenv("EXAMPLE_APP");
        page.navigate(Objects.requireNonNullElse(exampleApp, EXAMPLE_APP_URL));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
    }

    @AfterEach
    void closePage(TestInfo testInfo) {
        var screenshotPath = Paths.get(screenshotsDir, testInfo.getTestMethod().orElseThrow().getName() + ".png");
        page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
        page.close();
    }
}
