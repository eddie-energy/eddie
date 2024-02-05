package energy.eddie.tests.e2e.dk;


import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
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
    private static final String DK_ENERGINET_REFRESH_TOKEN = System.getenv("DK_ENERGINET_REFRESH_TOKEN");
    private static final String DK_ENERGINET_METERING_POINT = System.getenv("DK_ENERGINET_METERING_POINT");

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

    @Test
    void givenValidInput_showsAcceptedInfo() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Denmark")).locator("slot").nth(1).click();
        page.locator("sl-select").filter(new Locator.FilterOptions().setHasText("Energinet Permission")).locator("sl-popup div").first().click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Energinet")).locator("slot").nth(1).click();
        page.getByLabel("Refresh Token").fill(DK_ENERGINET_REFRESH_TOKEN);
        page.getByLabel("Metering Point").fill(DK_ENERGINET_METERING_POINT);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        assertThat(page.locator("sl-alert:nth-child(7) > .alert > .alert__message")).isVisible();
        assertThat(page.locator("dk-energinet-pa-ce")).containsText("Request completed! Your permission request was accepted.");
        assertThat(page.locator("dk-energinet-pa-ce")).containsText("The request status is: ACCEPTED");
    }
}