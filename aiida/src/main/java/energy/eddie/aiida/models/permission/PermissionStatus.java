package energy.eddie.aiida.models.permission;

public enum PermissionStatus {
    /**
     * The user accepted the permission request.
     */
    ACCEPTED,
    /**
     * The customer has accepted the permission but the start time for sharing data is still in the future.
     */
    WAITING_FOR_START,
    /**
     * The permission has been accepted and data is now actively streamed to the eligible party.
     */
    STREAMING_DATA,
    /**
     * The user rejected the permission request.
     */
    REJECTED,
    /**
     * The user requested a revocation of the permission.
     */
    REVOCATION_RECEIVED,
    /**
     * The user revoked the permission.
     */
    REVOKED,
    /**
     * The eligible party has requested a termination of the permission.
     */
    TERMINATION_RECEIVED,
    /**
     * The permission was terminated by the eligible party.
     */
    TERMINATED,
    /**
     * The permission request ran out of time.
     * The expiration of the permission was reached.
     */
    TIME_LIMIT
}
