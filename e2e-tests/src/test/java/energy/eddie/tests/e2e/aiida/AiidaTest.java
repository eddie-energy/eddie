package energy.eddie.tests.e2e.aiida;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AiidaTest extends E2eTestSetup {
    @Test
    void buttonClickShowsQrCode_andBase64() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(3).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue").setExact(true)).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        assertThat(page.getByLabel("{\"permissionId\":\"")).isVisible();
        assertThat(page.getByLabel("AIIDA code")).isVisible();
    }
}
