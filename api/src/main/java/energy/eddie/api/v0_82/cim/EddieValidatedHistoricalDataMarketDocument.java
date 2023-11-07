package energy.eddie.api.v0_82.cim;

import energy.eddie.cim.validated_historical_data.v0_82.ValidatedHistoricalDataMarketDocument;

import java.util.Optional;

/**
 * Specific implementation of {@link EddieMarketDocument} for validated historical data market documents.
 *
 * @param connectionId   The connection ID of the market document
 * @param permissionId   The permission ID of the market document
 * @param dataNeedId     The data need ID of the market document
 * @param marketDocument The validated historical data market document
 */
public record EddieValidatedHistoricalDataMarketDocument(
        Optional<String> connectionId,
        Optional<String> permissionId,
        Optional<String> dataNeedId,
        ValidatedHistoricalDataMarketDocument marketDocument
) implements EddieMarketDocument<ValidatedHistoricalDataMarketDocument> {
}