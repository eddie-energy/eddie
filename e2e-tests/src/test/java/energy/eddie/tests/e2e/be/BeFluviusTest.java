package energy.eddie.tests.e2e.be;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class BeFluviusTest extends E2eTestSetup {
    @Test
    void testSandbox() {
        this.navigateToRegionConnector(null, "Belgium", null);

        // Click create button
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create")).click();

        // Check if button shows the correct page
        assertThat(page.getByText("Permission granted")).isVisible();
    }
}
