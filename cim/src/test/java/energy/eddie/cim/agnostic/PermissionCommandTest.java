// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.agnostic;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link PermissionCommand#action()} returns the discriminator that matches each subtype's
 * {@code @JsonSubTypes} mapping.
 */
class PermissionCommandTest {
    private static final UUID PERMISSION_ID = UUID.fromString("a1f58555-4b87-4624-996c-ec4cf6ddb6c3");

    @Test
    void action_updateTransmissionSchedule_returnsUpdateTransmissionSchedule() {
        var command = new PermissionCommand.UpdateTransmissionSchedule("rc-1", PERMISSION_ID, "0 */1 * * * *");

        assertEquals(PermissionCommand.Action.UPDATE_TRANSMISSION_SCHEDULE, command.action());
    }

    @Test
    void action_setTransmissionEnabled_returnsSetTransmissionEnabled() {
        var command = new PermissionCommand.SetTransmissionEnabled("rc-1", PERMISSION_ID, true);

        assertEquals(PermissionCommand.Action.SET_TRANSMISSION_ENABLED, command.action());
    }

    @Test
    void action_terminate_returnsTerminate() {
        var command = new PermissionCommand.Terminate("rc-1", PERMISSION_ID);

        assertEquals(PermissionCommand.Action.TERMINATE, command.action());
    }

    @Test
    void action_nameMatchesJsonDiscriminator() {
        var command = new PermissionCommand.UpdateTransmissionSchedule("rc-1", PERMISSION_ID, "0 */1 * * * *");

        // The Action enum name is the wire discriminator emitted in the "action" JSON field.
        assertEquals("UPDATE_TRANSMISSION_SCHEDULE", command.action().name());
    }
}