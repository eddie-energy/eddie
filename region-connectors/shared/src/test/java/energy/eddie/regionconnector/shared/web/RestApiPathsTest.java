// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.web;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.connectionStatusMessagesStreamFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RestApiPathsTest {
    @Test
    void givenMultiplePermissionIds_whenConnectionStatusMessagesStreamForIsCalled_thenReturnsUriWithMultiplePermissionIdQueryParameters() {
        // Given
        var expected = URI.create("/api/connection-status-messages?permission-id=1&permission-id=2");

        // When
        var res = connectionStatusMessagesStreamFor("1", "2");

        // Then
        assertEquals(expected, res);
    }
}