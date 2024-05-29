package energy.eddie.tests.e2e.at;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AtEdaTest extends E2eTestSetup {
    @Test
    void buttonClick_statusIsPending() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();

        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Austria")).locator("slot").nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Permission Administrator")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Netz Niederösterreich GmbH"))
            .locator("slot")
            .nth(1)
            .click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        assertThat(page.locator("at-eda-pa-ce")).containsText(Pattern.compile(
                "The Consent Request ID for this connection is: [A-Z0-9]{8}"));

        var locator = page.getByText(
                "response code 99");
        locator.waitFor(new Locator.WaitForOptions().setTimeout(180_000));
    }
}
