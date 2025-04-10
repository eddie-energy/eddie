package energy.eddie.tests.e2e.nl;

// import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.tests.e2e.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static energy.eddie.tests.e2e.PlaywrightOptions.E2E_BASE_URL;

class NlMijnAansluitingTest extends E2eTestSetup {
//    @Test
//    void testValid() {
//        this.navigateToRegionConnector(null, "Netherlands", null);
//
//        // Shows permission form
//        page.getByLabel("House Number").fill("12");
//        Page callbackPage = context.waitForPage(() -> {
//            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();
//        });
//
//        // Select test user if not cached by local test
//        if (callbackPage.url().contains("https://www.acc.mijnenergiedata.nl/edlp/login")) {
//            callbackPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Testuser 11")).click();
//        }
//        // Interact with external permission page
//        callbackPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Deel mijn data")).click();
//        callbackPage.evaluate("document.location = document.URL.replace('https://eddie.projekte.fh-hagenberg.at/region-connectors/nl-edsn/authorization-callback','%s/region-connectors/nl-mijn-aansluiting/oauth2/code/mijn-aansluiting');".formatted(E2E_BASE_URL));
//
//        // Check if success message is shown on redirect page
//        assertThat(callbackPage.getByText("Access Granted. You can close this tab now.")).isVisible();
//
//        // Check if button shows the correct page
//        assertThat(page.getByText("Permission granted")).isVisible();
//    }

    @Test
    void testValid() {
        this.navigateToRegionConnector(null, "Netherlands", null);

        // Add debug listeners
        page.onConsoleMessage(msg -> System.out.println("Browser console: " + msg.text()));
        page.onPageError(err -> System.err.println("Page error: " + err));

        // Shows permission form
        assertThat(page.getByLabel("House Number")).isVisible();
        page.getByLabel("House Number").fill("12");

        // Give Playwright time to react (optional)
        page.waitForTimeout(1000);

        // Ensure Submit button is present
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit"))).isVisible();

        // Set timeout globally before using waitForPage (optional)
        context.setDefaultTimeout(10000); // 10s timeout for all waiters

        // Submit and wait for the popup/new tab
        Page callbackPage = context.waitForPage(() ->
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click()
        );

        // Optional delay to inspect behavior visually
        callbackPage.waitForTimeout(1000);

        // If redirected to login page, select user
        if (callbackPage.url().contains("https://www.acc.mijnenergiedata.nl/edlp/login")) {
            callbackPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Testuser 11")).click();
        }

        // Grant permission
        callbackPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Deel mijn data")).click();

        // Simulate redirect to callback
        callbackPage.evaluate("document.location = document.URL.replace('https://eddie.projekte.fh-hagenberg.at/region-connectors/nl-edsn/authorization-callback','%s/region-connectors/nl-mijn-aansluiting/oauth2/code/mijn-aansluiting');".formatted(E2E_BASE_URL));

        // Wait and assert for final message
        assertThat(callbackPage.getByText("Access Granted. You can close this tab now.")).isVisible();
        assertThat(page.getByText("Permission granted")).isVisible();
    }


    @Test
    void testInvalidHouseNumber() {
        this.navigateToRegionConnector(null, "Netherlands", null);

        page.getByLabel("House Number").fill("invalid");
        Page callbackPage = context.waitForPage(() -> {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();
        });

        callbackPage.evaluate("document.location = document.URL.replace('https://eddie.projekte.fh-hagenberg.at/region-connectors/nl-edsn/authorization-callback','%s/region-connectors/nl-mijn-aansluiting/oauth2/code/mijn-aansluiting');".formatted(E2E_BASE_URL));
        assertThat(callbackPage.getByText("Unable to complete request. You can close this tab now.")).isVisible();

        assertThat(page.getByText("Request was declined as invalid")).isVisible();
    }
}
