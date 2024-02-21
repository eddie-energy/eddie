package energy.eddie.tests.e2e.fr;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class FrEnedisTest extends E2eTestSetup {
    @Test
    void buttonClickOpensNewPage_statusIsPending() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("France")).locator("slot").nth(1).click();

        // When navigating to new page, set the variable page to the new page, to make sure that the new page gets screenshotted if something goes wrong on the new page
        var oldPage = page;
        page = page.waitForPopup(() -> page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName("Share your ENEDIS Link data")).click());

        // TODO GH-663 update E2E test to accept permission on Enedis page and check that status is then ACCEPTED; ALso include an assertion that checks if we are get redirected to the login page and not a random maintenance site
        page.close();
        page = oldPage;
        assertThat(page.locator("fr-enedis-pa-ce")).containsText("The request status is: PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT");
    }
}
