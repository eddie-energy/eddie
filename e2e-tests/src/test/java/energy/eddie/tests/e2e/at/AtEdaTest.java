package energy.eddie.tests.e2e.at;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AtEdaTest extends E2eTestSetup {
    @Test
    void buttonClick_statusIsPending() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();

        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Austria")).locator("slot").nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Permission Administrator")).click();

        var allAustrianDso = page.querySelectorAll("sl-option[value^='AT']");
        ElementHandle randomDsoToUse = allAustrianDso.get(new Random().nextInt(allAustrianDso.size()));
        randomDsoToUse.click();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        assertThat(page.locator("at-eda-pa-ce")).containsText(Pattern.compile(
                "The Consent Request ID for this connection is: [A-Z0-9]{8}"));

        var locator = page.getByText(
                "Status: Request Sent The permission request is now being processed by the permission administrator. Message was successfully received");
        locator.waitFor(new Locator.WaitForOptions().setTimeout(120_000));
    }
}
