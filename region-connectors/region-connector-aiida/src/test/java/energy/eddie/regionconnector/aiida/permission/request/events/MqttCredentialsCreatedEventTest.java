// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.permission.request.events;

import org.junit.jupiter.api.Test;

import static energy.eddie.api.v0.PermissionProcessStatus.ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MqttCredentialsCreatedEventTest {
    @Test
    void newMqttCredentialsCreatedEvent_usesStatusAccepted() {
        // When
        MqttCredentialsCreatedEvent event = new MqttCredentialsCreatedEvent("foo");

        // Then
        assertEquals(ACCEPTED, event.status());
        assertEquals("foo", event.permissionId());
    }
}
