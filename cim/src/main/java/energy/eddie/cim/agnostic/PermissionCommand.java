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
        @JsonSubTypes.Type(value = PermissionCommand.UpdateTransmissionSchedule.class, name = "UPDATE_TRANSMISSION_SCHEDULE"),
        @JsonSubTypes.Type(value = PermissionCommand.SetTransmissionEnabled.class, name = "SET_TRANSMISSION_ENABLED"),
        @JsonSubTypes.Type(value = PermissionCommand.Terminate.class, name = "TERMINATE")
})
public sealed interface PermissionCommand permits
        PermissionCommand.UpdateTransmissionSchedule,
        PermissionCommand.SetTransmissionEnabled,
        PermissionCommand.Terminate {

    default Action action() {
        return switch (this) {
            case UpdateTransmissionSchedule ignored -> Action.UPDATE_TRANSMISSION_SCHEDULE;
            case SetTransmissionEnabled ignored -> Action.SET_TRANSMISSION_ENABLED;
            case Terminate ignored -> Action.TERMINATE;
        };
    }

    String regionConnectorId();

    UUID permissionId();

    enum Action {
        UPDATE_TRANSMISSION_SCHEDULE(true),
        SET_TRANSMISSION_ENABLED(true),
        TERMINATE(false);

        private final boolean requiresExplicitGrant;

        Action(boolean requiresExplicitGrant) {
            this.requiresExplicitGrant = requiresExplicitGrant;
        }

        /**
         * Whether this command may only be executed if the data need explicitly grants it via
         * {@code allowedPermissionCommands}. Commands that return {@code false} (e.g. {@code TERMINATE})
         * are always accepted.
         */
        public boolean requiresExplicitGrant() {
            return requiresExplicitGrant;
        }
    }

    /**
     * Adjusts the transmission schedule for a permission.
     *
     * @param transmissionSchedule cron expression; effective schedule is capped at the data-need frequency
     */
    record UpdateTransmissionSchedule(
            String regionConnectorId,
            UUID permissionId,
            String transmissionSchedule
    ) implements PermissionCommand {
    }

    /**
     * Enables or disables transmission for a permission.
     *
     * @param enabled whether transmission should be enabled or disabled
     */
    record SetTransmissionEnabled(
            String regionConnectorId,
            UUID permissionId,
            boolean enabled
    ) implements PermissionCommand {
    }

    /**
     * Terminates a permission, causing the region connector to stop transmitting data for it and clean up any associated resources.
     */
    record Terminate(
            String regionConnectorId,
            UUID permissionId
    ) implements PermissionCommand {
    }
}