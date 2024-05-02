package energy.eddie.api.v0_82;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of {@link EddieAccountingPointMarketDocument}s available.
 */
public interface EddieAccountingPointMarketDocumentProvider extends AutoCloseable {
    /**
     * Data stream of all EddieValidatedHistoricalDataMarketDocument created by this region connector.
     *
     * @return EddieValidatedHistoricalDataMarketDocument stream
     */
    Flux<EddieAccountingPointMarketDocument> getEddieAccountingPointMarketDocumentStream();

    /**
     * Emit a complete signal on the Flow in this method.
     */
    @Override
    void close() throws Exception;
}
