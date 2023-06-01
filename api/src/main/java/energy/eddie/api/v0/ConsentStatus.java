package energy.eddie.api.v0;

/**
 * Status of a consent process.
 */
public enum ConsentStatus {
    /** A new consent process was created but no request is sent to the PA yet. */
    NEW,
    /** The permission request was sent to the PA but there is no reply yet. */
    PENDING,
    /** Permission request was processed by the PA and the permission is granted. */
    ACCEPTED,
    /** Permission request was processed by the PA and the permission was not granted. */
    REJECTED,
    /** An error occured during the consent process. (on the PA or on the region connector side) */
    ERROR
}
