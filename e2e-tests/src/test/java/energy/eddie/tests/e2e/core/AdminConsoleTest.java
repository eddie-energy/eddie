package energy.eddie.tests.e2e.core;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import energy.eddie.tests.e2e.PlaywrightOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static energy.eddie.tests.e2e.PlaywrightOptions.BASE_URL;

@UsePlaywright(PlaywrightOptions.class)
class AdminConsoleTest {
    private static final String ADMIN_URL = BASE_URL.replace("8080", "9090") + "/outbound-connectors/admin-console";
    private static final String LOGIN_URL = ADMIN_URL + "/login";

    @Test
    void redirectsToLogin(Page page) {
        page.navigate(ADMIN_URL);
        page.waitForURL(LOGIN_URL);
        assertThat(page).hasURL(LOGIN_URL);
    }

    @Test
    void visibleOnSuccessfulLogin(Page page) {
        page.navigate(ADMIN_URL);
        page.waitForURL(LOGIN_URL);
        page.getByLabel("Username").fill("admin");
        page.getByLabel("Password").fill("password");
        page.getByText("Log in").click();
        page.waitForURL(ADMIN_URL + "?continue");
        assertThat(page).hasURL(ADMIN_URL + "?continue");
        assertThat(page).hasTitle("Admin Console");
    }

    @AfterEach
    void saveScreenshot(Page page, TestInfo testInfo) {
        var screenshotPath = Paths.get("build/playwright-results/",
                                       getClass().getSimpleName(),
                                       testInfo.getDisplayName() + ".png");
        page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
    }
}
