package energy.eddie.api.v0_82;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;

import java.util.concurrent.Flow;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link EddieValidatedHistoricalDataMarketDocument}s available.
 */
public interface CimConsumptionRecordProvider extends AutoCloseable {
    /**
     * Data stream of all EddieValidatedHistoricalDataMarketDocument created by this region connector.
     *
     * @return EddieValidatedHistoricalDataMarketDocument stream that can be consumed only once
     */
    Flow.Publisher<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream();

    /**
     * Emit a complete signal on the Flow in this method.
     */
    @Override
    void close() throws Exception;
}
