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
    void action_updateSchedule_returnsUpdateScheduleDiscriminator() {
        PermissionCommand command = new PermissionCommand.UpdateSchedule("rc-1",
                                                                         PERMISSION_ID,
                                                                         "0 */1 * * * *");

        assertEquals(PermissionCommand.UPDATE_SCHEDULE, command.action());
    }

    @Test
    void action_setTransmissionEnabled_returnsSetTransmissionEnabledDiscriminator() {
        PermissionCommand command = new PermissionCommand.SetTransmissionEnabled("rc-1",
                                                                                 PERMISSION_ID,
                                                                                 true);

        assertEquals(PermissionCommand.SET_TRANSMISSION_ENABLED, command.action());
    }

    @Test
    void action_terminate_returnsTerminateDiscriminator() {
        PermissionCommand command = new PermissionCommand.Terminate("rc-1", PERMISSION_ID);

        assertEquals(PermissionCommand.TERMINATE, command.action());
    }
}