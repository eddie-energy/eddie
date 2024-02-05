package energy.eddie.tests.e2e.fr;

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

class FrEnedisTest {
    private static final String OUT_DIR = TestConfig.OUT_DIR_BASE_PATH + FrEnedisTest.class.getSimpleName();
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
        page.setViewportSize(1920, 2080);
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
    void frEnedis_buttonClickOpensNewPage_statusIsPending() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("France")).locator("slot").nth(1).click();
        page.locator("sl-select").filter(new Locator.FilterOptions().setHasText("Enedis SRD Energies")).locator("sl-popup div").first().click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Enedis")).locator("slot").nth(1).click();

        // When navigating to new page, set the variable page to the new page, to make sure that the new page gets screenshotted if something goes wrong on the new page
        var oldPage = page;
        page = page.waitForPopup(() -> page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("Share your ENEDIS Link data")).click());

        // TODO GH-663 update E2E test to accept permission on Enedis page and check that status is then ACCEPTED; ALso include an assertion that checks if we are get redirected to the login page and not a random maintenance site
        page.close();
        page = oldPage;
        assertThat(page.locator("fr-enedis-pa-ce")).containsText("The request status is: PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT");
    }
}
