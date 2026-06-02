// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.agnostic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Permission control signal sent by the eligible party to a certain region connector.
 *
 * <p>The {@code action} field acts as the polymorphic discriminator; each action carries its own
 * payload. Consumers should pattern-match over the permitted subtypes.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PermissionCommand.UpdateSchedule.class, name = "UPDATE_SCHEDULE"),
        @JsonSubTypes.Type(value = PermissionCommand.SetTransmissionEnabled.class, name = "SET_TRANSMISSION_ENABLED")
})
public sealed interface PermissionCommand
        permits PermissionCommand.UpdateSchedule,
        PermissionCommand.SetTransmissionEnabled {

    String regionConnectorId();

    UUID permissionId();

    ZonedDateTime timestamp();

    /**
     * Adjusts the transmission schedule for a permission.
     *
     * @param transmissionSchedule cron expression; effective schedule is capped at the data-need frequency
     */
    record UpdateSchedule(
            String regionConnectorId,
            UUID permissionId,
            ZonedDateTime timestamp,
            String transmissionSchedule
    ) implements PermissionCommand {
    }

    /**
     * Enables or disables transmission for a permission.
     */
    record SetTransmissionEnabled(
            String regionConnectorId,
            UUID permissionId,
            ZonedDateTime timestamp,
            boolean enabled
    ) implements PermissionCommand {
    }
}