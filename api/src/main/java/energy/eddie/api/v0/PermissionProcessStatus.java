package energy.eddie.api.v0;

/**
 * Implements the status of the <a href="https://github.com/eddie-energy/eddie/wiki/EDDIE-Consent-Process-Model">Consent Process Model</a>.
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
     * The permission request has been sent to the permission administrator.
     * The process waits for an answer by the PA.
     */
    SENT_TO_PERMISSION_ADMINISTRATOR,
    /**
     * Not implemented or fully defined.
     * To be done in <a href="https://github.com/eddie-energy/eddie/issues/177">GH-177</a>.
     */
    CANCELLED,
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
     * The permission request ran out of time.
     * The expiration of the permission was reached.
     */
    TIME_LIMIT
}
