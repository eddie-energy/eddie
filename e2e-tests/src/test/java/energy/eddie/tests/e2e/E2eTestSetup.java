package energy.eddie.tests.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Paths;

/**
 * This E2E base test class setups the playwright environment and automatically takes a screenshot of <code>page</code>
 * when an error occurs or at the end of the test. Initially, the example app main screen is loaded as
 * <code>page</code>.
 */
@UsePlaywright(PlaywrightOptions.class)
public class E2eTestSetup {
    protected Page page;

    protected void navigateToRegionConnector(String dataNeed, String country, String permissionAdministrator) {
        if (dataNeed != null) {
            page.getByLabel("Data need").selectOption(dataNeed);
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

    @BeforeEach
    void navigateToDemoPage(Page page) {
        this.page = page;
        page.navigate("/demo");
    }

    @AfterEach
    void saveScreenshot(TestInfo testInfo) {
        var screenshotPath = Paths.get("build/playwright-results/",
                                       getClass().getSimpleName(),
                                       testInfo.getDisplayName() + ".png");
        page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
    }
}
