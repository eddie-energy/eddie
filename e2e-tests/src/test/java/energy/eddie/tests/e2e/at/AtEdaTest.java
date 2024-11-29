package energy.eddie.tests.e2e.at;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AtEdaTest extends E2eTestSetup {
    @Test
    void withoutAccountingPointId() {
        this.navigateToRegionConnector(null, "Austria", "Netz Niederösterreich GmbH");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        page.getByText("Your permission request was created successfully.");

        var locator = page.getByText("Your request was successfully sent");
        locator.waitFor(new Locator.WaitForOptions().setTimeout(120_000));
        assertThat(locator).isVisible();

        // Continue button should show the name of the PA
        page.getByText("Continue to Netz Niederösterreich").click();
    }
}
