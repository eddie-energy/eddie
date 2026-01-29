// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RedirectUriJsonNodeTest {
    @Test
    void testApply_onEmptyRedirectUri_returnsNull() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .setRedirectUri(null)
                .build();
        var func = new RedirectUriJsonNode(new ObjectMapper());

        // When
        var res = func.apply(pr);

        // Then
        assertNull(res);
    }

    @Test
    void testApply_onInvalidState_returnsNull() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setStatus(PermissionProcessStatus.CREATED)
                .setRedirectUri("https://localhost")
                .build();
        var func = new RedirectUriJsonNode(new ObjectMapper());

        // When
        var res = func.apply(pr);

        // Then
        assertNull(res);
    }

    @Test
    void testApply_onValidPermissionRequest_returnsJsonNode() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                .setRedirectUri("https://localhost")
                .build();
        var func = new RedirectUriJsonNode(new ObjectMapper());

        // When
        var res = func.apply(pr);

        // Then
        assertNotNull(res);
    }
}