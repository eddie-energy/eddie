package energy.eddie.tests.e2e.dk;


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
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.locator("div:nth-child(6) > .uk-display-block > sl-dialog > sl-select > .form-control > .form-control-input > .select > .select__combobox").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Denmark")).locator("slot").nth(1).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        page.getByLabel("Refresh Token").fill("bla");
        page.getByLabel("Refresh Token").press("Tab");
        page.getByLabel("Metering Point").fill("blu");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        assertThat(page.getByRole(AriaRole.PARAGRAPH)).containsText("An error occurred The given refresh token is not valid.");
    }

    @Test
    void givenValidInput_showsAcceptedInfo() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Denmark")).locator("slot").nth(1).click();
        page.getByLabel("Refresh Token").fill(DK_ENERGINET_REFRESH_TOKEN);
        page.getByLabel("Metering Point").fill(DK_ENERGINET_METERING_POINT);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();
        assertThat(page.locator("sl-alert:nth-child(7) > .alert > .alert__message")).isVisible();
        assertThat(page.locator("dk-energinet-pa-ce")).containsText("Request completed! Your permission request was accepted.");
        assertThat(page.locator("dk-energinet-pa-ce")).containsText("The request status is: ACCEPTED");
    }
}