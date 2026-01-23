// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.TestEvent;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FulfillmentServiceTest {
    @Mock
    private Outbox outbox;

    @Test
    void testTryFulfillPermissionRequest_emitsFulfilledEvent() {
        // Given
        var service = new FulfillmentService(outbox, TestEvent::new);
        var permissionRequest = new SimplePermissionRequest(
                "pid", "cid", "dnid", null, null, ZonedDateTime.now(ZoneOffset.UTC),
                PermissionProcessStatus.ACCEPTED
        );

        // When
        service.tryFulfillPermissionRequest(permissionRequest);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.FULFILLED, event.status())));
    }
}