package energy.eddie.tests.e2e.dk;


import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class DkEnerginetTest extends E2eTestSetup {
    private static final String DK_ENERGINET_REFRESH_TOKEN = System.getenv("DK_ENERGINET_REFRESH_TOKEN");
    private static final String DK_ENERGINET_METERING_POINT = System.getenv("DK_ENERGINET_METERING_POINT");

    @Test
    void givenInvalidRefreshToken_displaysErrorMessage() {
        this.navigateToRegionConnector(null, "Denmark", null);

        page.getByLabel("Metering Point").fill("foo");
        page.getByLabel("Refresh Token").fill("bar");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        var locator = page.locator("body", new Page.LocatorOptions().setHasText("An error occurred"));
        locator.waitFor(new Locator.WaitForOptions().setTimeout(120_000));  // 2 min
        assertThat(locator).containsText(
                "An error occurred Error: Refresh Token is either malformed or is not valid until the end of the requested permission"
        );
    }

    @Test
    void givenValidInput_showsAcceptedInfo() {
        this.navigateToRegionConnector(null, "Denmark", null);

        page.getByLabel("Refresh Token").fill(DK_ENERGINET_REFRESH_TOKEN);
        page.getByLabel("Metering Point").fill(DK_ENERGINET_METERING_POINT);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        var locator = page.getByText("Permission granted");
        locator.waitFor(new Locator.WaitForOptions().setTimeout(120_000));  // 2 min

        assertThat(locator).isVisible();
    }
}
