// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.e2etests.regionconnector;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.e2etests.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AtEdaTest extends E2eTestSetup {
    @Test
    void withoutAccountingPointId() {
        this.navigateToRegionConnector(null, "Austria", "Netz Niederösterreich GmbH");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        page.getByText("1 of 1 permission requests was created successfully.");

        var locator = page.getByText("1 of 1 requests were successfully sent");
        locator.waitFor(new Locator.WaitForOptions().setTimeout(120_000));
        assertThat(locator).isVisible();

        // Continue button should show the name of the PA
        page.getByText("Continue to Netz Niederösterreich").click();
    }
}
