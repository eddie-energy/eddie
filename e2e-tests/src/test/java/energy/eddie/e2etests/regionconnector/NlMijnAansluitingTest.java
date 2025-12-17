package energy.eddie.e2etests.regionconnector;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.e2etests.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static energy.eddie.e2etests.PlaywrightOptions.BASE_URL;

class NlMijnAansluitingTest extends E2eTestSetup {
    @Test
    void testValid() {
        this.navigateToRegionConnector(null, "Netherlands", null);

        // Shows permission form
        page.getByLabel("House Number").fill("2");
        Page callbackPage = page.context().waitForPage(() -> {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();
        });

        // Select test user if not cached by local test
        if (callbackPage.url().contains("https://www.acc.mijnenergiedata.nl/edlp/login")) {
            callbackPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Testuser veertien")).click();
        }
        // Interact with external permission page
        callbackPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Deel mijn data")).click();
        callbackPage.evaluate("document.location = document.URL.replace('https://eddie.projekte.fh-hagenberg.at/region-connectors/nl-edsn/authorization-callback','%s/region-connectors/nl-mijn-aansluiting/oauth2/code/mijn-aansluiting');".formatted(BASE_URL));

        // Check if success message is shown on redirect page
        assertThat(callbackPage.getByText("Access granted. You can close this tab now.")).isVisible();

        // Check if button shows the correct page
        assertThat(page.getByText("Permission granted")).isVisible();
    }

    @Test
    void testInvalidHouseNumber() {
        this.navigateToRegionConnector(null, "Netherlands", null);

        page.getByLabel("House Number").fill("invalid");
        Page callbackPage = page.context().waitForPage(() -> {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();
        });

        callbackPage.evaluate("document.location = document.URL.replace('https://eddie.projekte.fh-hagenberg.at/region-connectors/nl-edsn/authorization-callback','%s/region-connectors/nl-mijn-aansluiting/oauth2/code/mijn-aansluiting');".formatted(BASE_URL));
        assertThat(callbackPage.getByText("Invalid answer. Please contact the service provider.")).isVisible();

        assertThat(page.getByText("Request was declined as invalid")).isVisible();
    }
}
