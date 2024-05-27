package energy.eddie.aiida.models.permission;

public enum PermissionStatus {
    /**
     * The permission has been created on AIIDA.
     */
    CREATED,
    /**
     * The details, like start and end date, of the permission have been fetched from the EDDIE framework.
     */
    FETCHED_DETAILS,
    /**
     * This AIIDA instance is unable to fulfill the permission request, e.g. because the requested data is not
     * available.
     */
    UNFULFILLABLE,
    /**
     * The user accepted the permission request.
     */
    ACCEPTED,
    /**
     * The MQTT credentials and topics for the permission have been fetched from the EDDIE framework.
     */
    FETCHED_MQTT_CREDENTIALS,
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
     * The permission was terminated by the eligible party.
     */
    TERMINATED,
    /**
     * The expiration time of the permission was reached.
     */
    FULFILLED,
    /**
     * An error occurred and the permission could not be started.
     */
    FAILED_TO_START
}
