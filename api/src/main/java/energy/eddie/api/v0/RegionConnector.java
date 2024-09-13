package energy.eddie.api.v0;

import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;

/**
 * A region connector connects MDAs to EDDIE. It implements the MDA specific processes and converts requests for
 * permissions to the format expected by the MDA and also implements MDA and CA specifics related to these MDAs. If
 * offers:
 * <ul>
 *     <li>metadata describing the region connector and the supported MDAs</li>
 *     <li>a web-UI component for the permission process</li>
 *     <li>methods for managing permissions</li>
 * </ul>
 * <p>
 * The interfaces in the see also section extend the functionality of the region connector.
 * Note that there may be more versions than are referenced.
 *
 * @see ConnectionStatusMessageProvider
 * @see energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider
 * @see energy.eddie.api.v0_82.AccountingPointEnvelopeProvider
 * @see energy.eddie.api.v0_82.PermissionMarketDocumentProvider
 */
public interface RegionConnector {
    /**
     * Get metadata describing this region connector.
     *
     * @return metadata object
     */
    RegionConnectorMetadata getMetadata();

    /**
     * Terminates the permission at the permission administrator's system and closes the associated connection.
     *
     * @param permissionId unique id of the permission
     */
    void terminatePermission(String permissionId);
}
