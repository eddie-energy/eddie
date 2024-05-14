package energy.eddie.api.v0_82.cim;

import energy.eddie.cim.v0_82.ap.AccountingPointMarketDocument;

/**
 * Specific implementation of {@link EddieMarketDocument} for validated historical data market documents.
 *
 * @param connectionId   The connection ID of the market document
 * @param permissionId   The permission ID of the market document
 * @param dataNeedId     The data need ID of the market document
 * @param marketDocument The validated historical data market document
 */
public record EddieAccountingPointMarketDocument(
        String connectionId,
        String permissionId,
        String dataNeedId,
        AccountingPointMarketDocument marketDocument
) implements EddieMarketDocument<AccountingPointMarketDocument> {
}
