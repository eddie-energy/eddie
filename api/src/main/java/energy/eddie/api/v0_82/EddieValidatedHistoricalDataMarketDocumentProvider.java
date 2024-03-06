package energy.eddie.api.v0_82;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link EddieValidatedHistoricalDataMarketDocument}s available.
 */
public interface EddieValidatedHistoricalDataMarketDocumentProvider extends AutoCloseable {
    /**
     * Data stream of all EddieValidatedHistoricalDataMarketDocument created by this region connector.
     *
     * @return EddieValidatedHistoricalDataMarketDocument stream
     */
    Flux<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream();

    /**
     * Emit a complete signal on the Flow in this method.
     */
    @Override
    void close() throws Exception;
}