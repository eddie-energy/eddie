// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v0;

/**
 * Implements the status of the <a href="https://architecture.eddie.energy/framework/2-integrating/integrating.html#permission-process-model">Permission Process Model</a>.
 */
public enum PermissionProcessStatus {
    /**
     * Permission Request has been created, but not validated or sent to a permission administrator
     */
    CREATED,
    /**
     * The permission request has been validated.
     * It contains only valid values at this point of the process.
     */
    VALIDATED,
    /**
     * The permission request is malformed.
     * If the user tries to re-send we can reattempt to validate it.
     */
    MALFORMED,
    /**
     * We are unable to send the permission request to the permission administrator.
     * Could be due to a service outage or a network issue.
     */
    UNABLE_TO_SEND,
    /**
     * The permission request is now being processed by the PA.
     */
    SENT_TO_PERMISSION_ADMINISTRATOR,
    /**
     * A user can forgo the option of accepting a permission request.
     * In that case the request will time out.
     */
    TIMED_OUT,
    /**
     * The user accepted the permission request.
     */
    ACCEPTED,
    /**
     * The user rejected the permission request.
     */
    REJECTED,
    /**
     * The permission request is not formatted correctly or contains semantic errors and is invalid.
     */
    INVALID,
    /**
     * The user revoked the permission via the permission administrator or the permission administrator themselves removed the permission.
     */
    REVOKED,
    /**
     * The permission was terminated via the eligible party.
     */
    TERMINATED,
    /**
     * The permission request has been fulfilled, i.e. all data has been delivered.
     */
    FULFILLED,
    /**
     * The permission request specifies energy data that is not available for that final customer.
     */
    UNFULFILLABLE,
    /**
     * A follow-up state for {@code UNFULFILLABLE, FULFILLED, TERMINATED}, since sometimes these states have to be
     * externally terminated.
     */
    REQUIRES_EXTERNAL_TERMINATION,
    /**
     * The external termination process failed.
     */
    FAILED_TO_TERMINATE,
    /**
     * The external termination process was successful.
     */
    EXTERNALLY_TERMINATED
}