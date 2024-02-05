package energy.eddie.tests.e2e.dk;


import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.TestConfig;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static energy.eddie.tests.e2e.TestConfig.EXAMPLE_APP_URL;

class DkEnerginetTest {
    private static final String OUT_DIR = TestConfig.OUT_DIR_BASE_PATH + DkEnerginetTest.class.getSimpleName();
    private static Playwright playwright;
    private static Browser browser;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @BeforeEach
    void openPage() {
        page = browser.newPage();
        page.setViewportSize(1920, 2080);    // big to make sure everything is visible on the screenshots --> TODO how to programmatically scroll to bottom of dialog?
        page.navigate(EXAMPLE_APP_URL);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
    }

    @AfterEach
    void closePage(TestInfo testInfo) {
        var screenshotPath = Paths.get(OUT_DIR, testInfo.getTestMethod().orElseThrow().getName() + ".png");
        page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
        page.close();
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @Test
    void dkEnerginet_invalidRefreshToken_displaysErrorMessage() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.locator("div:nth-child(6) > .uk-display-block > sl-dialog > sl-select > .form-control > .form-control-input > .select > .select__combobox").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Denmark")).locator("slot").nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Permission Administrator")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Energinet")).locator("slot").nth(1).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        page.getByLabel("Refresh Token").fill("bla");
        page.getByLabel("Refresh Token").press("Tab");
        page.getByLabel("Metering Point").fill("blu");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        assertThat(page.getByRole(AriaRole.PARAGRAPH)).containsText("An error occurred The given refresh token is not valid.");
    }
}