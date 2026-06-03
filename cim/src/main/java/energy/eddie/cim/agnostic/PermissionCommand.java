// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.agnostic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

/**
 * Permission control signal sent by the eligible party to a certain region connector.
 *
 * <p>The {@code action} field acts as the polymorphic discriminator; each action carries its own
 * payload. Consumers should pattern-match over the permitted subtypes.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PermissionCommand.UpdateSchedule.class, name = PermissionCommand.UPDATE_SCHEDULE),
        @JsonSubTypes.Type(value = PermissionCommand.SetTransmissionEnabled.class, name = PermissionCommand.SET_TRANSMISSION_ENABLED),
        @JsonSubTypes.Type(value = PermissionCommand.Terminate.class, name = PermissionCommand.TERMINATE)
})
public sealed interface PermissionCommand permits
        PermissionCommand.UpdateSchedule,
        PermissionCommand.SetTransmissionEnabled,
        PermissionCommand.Terminate {

    String UPDATE_SCHEDULE = "UPDATE_SCHEDULE";
    String SET_TRANSMISSION_ENABLED = "SET_TRANSMISSION_ENABLED";
    String TERMINATE = "TERMINATE";

    String regionConnectorId();

    UUID permissionId();

    default boolean controlsTransmission() {
        return false;
    }

    default String action() {
        return switch (this) {
            case UpdateSchedule ignored -> UPDATE_SCHEDULE;
            case SetTransmissionEnabled ignored -> SET_TRANSMISSION_ENABLED;
            case Terminate ignored -> TERMINATE;
        };
    }

    /**
     * Adjusts the transmission schedule for a permission.
     *
     * @param transmissionSchedule cron expression; effective schedule is capped at the data-need frequency
     */
    record UpdateSchedule(
            String regionConnectorId,
            UUID permissionId,
            String transmissionSchedule
    ) implements PermissionCommand {
        @Override
        public boolean controlsTransmission() {
            return true;
        }
    }

    /**
     * Enables or disables transmission for a permission.
     */
    record SetTransmissionEnabled(
            String regionConnectorId,
            UUID permissionId,
            boolean enabled
    ) implements PermissionCommand {
        @Override
        public boolean controlsTransmission() {
            return true;
        }
    }

    record Terminate(
            String regionConnectorId,
            UUID permissionId
    ) implements PermissionCommand {
    }
}