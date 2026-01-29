// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.e2etests.regionconnector;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.e2etests.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class BeFluviusTest extends E2eTestSetup {
    @Test
    void testSandbox() {
        this.navigateToRegionConnector(null, "Belgium", null);

        // Click create button
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        // Check if button shows the correct page
        assertThat(page.getByText("Permission granted")).isVisible();
    }
}
