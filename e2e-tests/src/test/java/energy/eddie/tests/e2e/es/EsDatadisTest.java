package energy.eddie.tests.e2e.es;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class EsDatadisTest extends E2eTestSetup {
    @Test
    void givenInvalidNif_showsNifDoesNotExist() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(4).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Spain")).locator("slot").nth(1).click();
        page.getByLabel("DNI/Nif").click();
        page.getByLabel("DNI/Nif").fill("foo");
        page.getByLabel("DNI/Nif").press("Tab");
        page.getByLabel("CUPS").fill("bar");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        var locator = page.locator("es-datadis-pa-ce", new Page.LocatorOptions().setHasText("Permission request created! Your permission request was created successfully."));
        // Datadis API may take a long time to respond
        locator.waitFor(new Locator.WaitForOptions().setTimeout(120_000));  // 2 min
        assertThat(locator).containsText("Permission request created! Your permission request was created successfully.");
        assertThat(page.locator("es-datadis-pa-ce")).containsText("Request completed! The permission request was invalid. Reason: Given NIF does not exist");
        assertThat(page.locator("es-datadis-pa-ce")).containsText("The request status is: INVALID");
    }
}
