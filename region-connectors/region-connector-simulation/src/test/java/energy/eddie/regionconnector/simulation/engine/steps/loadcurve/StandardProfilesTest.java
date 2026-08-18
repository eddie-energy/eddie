// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.loadcurve;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardProfilesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "All daytime", "Early morning & evening", "Mid morning", "Midday peak", "Late afternoon", "Evening", "Midday trough"
    })
    void testParsesCorrectly(String profile) {
        // Given
        var stdProfiles = StandardProfiles.getInstance();
        // When
        var res = stdProfiles.getProfile(profile);

        // Then
        assertTrue(res.isPresent());
    }
}