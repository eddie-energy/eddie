package energy.eddie.tests.e2e.fi;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class FiFingridTest extends E2eTestSetup {
    @Test
    void testAccepted() {
        this.navigateToRegionConnector("LAST_3_MONTHS_HOURLY_MEASUREMENTS_PER_DAY", "Finland", null);

        page.getByLabel("Customer Identification").fill("eddie.developers@fh-hagenberg.at");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Accepted").setExact(true)).click();

        assertThat(page.getByText("Permission granted")).isVisible();
    }

    @Test
    void testRejected() {
        this.navigateToRegionConnector("LAST_3_MONTHS_HOURLY_MEASUREMENTS_PER_DAY", "Finland", null);

        page.getByLabel("Customer Identification").fill("eddie.developers@fh-hagenberg.at");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Rejected").setExact(true)).click();

        assertThat(page.getByText("Permission request rejected")).isVisible();
    }
}
