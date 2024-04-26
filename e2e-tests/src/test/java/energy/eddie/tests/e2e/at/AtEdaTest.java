package energy.eddie.tests.e2e.at;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AtEdaTest extends E2eTestSetup {

    private static final String SENT_TO_PERMISSION_ADMINISTRATOR = "SENT_TO_PERMISSION_ADMINISTRATOR";

    @Test
    void buttonClick_statusIsPending() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Austria")).locator("slot").nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Permission Administrator")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Netz Niederösterreich GmbH")).locator("slot").nth(1).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        assertThat(page.locator("sl-alert").filter(new Locator.FilterOptions().setHasText("Permission request created!")).locator("div").nth(2)).isVisible();
        assertThat(page.locator("at-eda-pa-ce")).containsText("The Consent Request ID for this connection is: ");
        var locator = page.locator("at-eda-pa-ce",
                                   new Page.LocatorOptions().setHasText(SENT_TO_PERMISSION_ADMINISTRATOR));
        locator.waitFor(new Locator.WaitForOptions());
        assertThat(page.locator("at-eda-pa-ce")).containsText(SENT_TO_PERMISSION_ADMINISTRATOR);
    }
}