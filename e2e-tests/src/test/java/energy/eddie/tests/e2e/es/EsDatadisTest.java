package energy.eddie.tests.e2e.es;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class EsDatadisTest extends E2eTestSetup {
    @Test
    void givenInvalidNif_showsNifDoesNotExist() {
        this.navigateToRegionConnector("LAST_3_MONTHS_HOURLY_MEASUREMENTS_PER_DAY", "Spain", null);

        page.getByLabel("DNI/Nif").fill("00000001T");
        page.getByLabel("CUPS").fill("bar");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        var locator = page.getByText("Error: Invalid NIF");
        assertThat(locator).isVisible();
    }
}
