package energy.eddie.tests.e2e.fr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class FrEnedisTest extends E2eTestSetup {
    public static final String SANDBOX_METERING_POINT_WITH_ACCEPTED_CONSENT = "22516914714270";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buttonClickOpensNewPage_statusIsFulfilled() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect with EDDIE")).nth(1).click();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Country")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("France")).locator("slot").nth(1).click();

        RequestDetails requestDetails = new RequestDetails();
        page.onResponse(response -> {
            if (response.url().contains("fr-enedis/permission-request")) {
                try {
                    var responseBody = objectMapper.readTree(response.body());
                    requestDetails.setPermissionId(responseBody.get("permissionId").asText());
                    requestDetails.setUrl(response.url().split("/permission-request", -1)[0]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // When navigating to new page, set the variable page to the new page, to make sure that the new page gets screenshotted if something goes wrong on the new page
        var buttonPage = page;
        page = page.waitForPopup(() -> page.getByRole(
                                                   AriaRole.IMG,
                                                   new Page.GetByRoleOptions().setName("Share your ENEDIS Link data")
                                           )
                                           .click());

        var redirectUrl = requestDetails.url() +
                          "/authorization-callback" +
                          "?state=" + requestDetails.permissionId() +
                          "&usage_point_id=" + SANDBOX_METERING_POINT_WITH_ACCEPTED_CONSENT;
        page.navigate(redirectUrl);
        page.close();
        page = buttonPage;

        var locator = buttonPage.locator("body", new Page.LocatorOptions().setHasText("Fulfilled"));
        locator.waitFor(new Locator.WaitForOptions().setTimeout(120_000));
        assertThat(locator).containsText(
                "Status: Request Fulfilled The permission request has been fulfilled, i.e. all data has been delivered.");
    }

    static class RequestDetails {
        private String permissionId;
        private String url;

        public String permissionId() {
            return permissionId;
        }

        public void setPermissionId(String permissionId) {
            this.permissionId = permissionId;
        }

        public String url() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
