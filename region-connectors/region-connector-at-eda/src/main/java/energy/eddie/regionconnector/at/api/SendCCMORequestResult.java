package energy.eddie.regionconnector.at.api;

/**
 * This class represents the result of a sendCCMORequest operation.
 * @param permissionId can be used to identify which StatusUpdates belong to this request.
 * @param cmRequestId can be used by the customer to identify and accept the request in the portal of his DSO.
 */
public record SendCCMORequestResult(String permissionId, String cmRequestId) {
}