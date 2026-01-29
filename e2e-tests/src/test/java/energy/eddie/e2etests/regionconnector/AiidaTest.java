// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.e2etests.regionconnector;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import energy.eddie.e2etests.E2eTestSetup;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AiidaTest extends E2eTestSetup {
    @Test
    void buttonClickShowsQrCode_andBase64() {
        this.navigateToRegionConnector("FUTURE_NEAR_REALTIME_DATA_OUTBOUND", null, null);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect").setExact(true)).click();

        assertThat(page.getByLabel("AIIDA QR code")).isVisible();
        assertThat(page.getByLabel("AIIDA code")).isVisible();
    }
}
