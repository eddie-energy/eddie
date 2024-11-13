package energy.eddie.tests.e2e.fr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class FrEnedisTest extends E2eTestSetup {
    public static final String SANDBOX_METERING_POINT_WITH_ACCEPTED_PERMISSION = "22516914714270";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Disabled("Until Enedis' sandbox is working again")
    void buttonClickOpensNewPage_statusIsFulfilled() {
        this.navigateToRegionConnector(null, "France", null);

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
        page = page.waitForPopup(() -> page.getByAltText("Share your ENEDIS Linky data").click());

        var redirectUrl = requestDetails.url() +
                          "/authorization-callback" +
                          "?state=" + requestDetails.permissionId() +
                          "&usage_point_id=" + SANDBOX_METERING_POINT_WITH_ACCEPTED_PERMISSION;
        page.navigate(redirectUrl);
        page.close();
        page = buttonPage;

        var locator = page.getByText("Permission granted");
        locator.waitFor(new Locator.WaitForOptions().setTimeout(120_000));  // 2 min

        assertThat(locator).isVisible();
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
