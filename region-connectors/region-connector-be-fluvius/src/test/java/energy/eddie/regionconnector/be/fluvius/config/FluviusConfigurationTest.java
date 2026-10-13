// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FluviusConfigurationTest {
    @Test
    void rejectsMockMandatesOnLiveApi() {
        assertThrows(IllegalArgumentException.class,
                     () -> new FluviusConfiguration("https://apihub.fluvius.be/esco-live", "key", "contract", true));
    }

    @Test
    void allowsMockMandatesOnSandboxApi() {
        var config = new FluviusConfiguration("https://apihub.fluvius.be/esco-sbx", "key", "contract", true);
        assertTrue(config.mockMandates());
    }
}
