package energy.eddie.regionconnector.de.eta.providers.v1_04;

import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.config.PlainDeConfiguration;
import energy.eddie.regionconnector.de.eta.dtos.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.streams.ValidatedHistoricalDataStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Provider for CIM v1.04 Validated Historical Data Market Documents.
 * Transforms validated historical data from the ETA Plus API into CIM v1.04 VHDEnvelope format.
 */
@Component("deValidatedHistoricalDataMarketDocumentProviderV104")
public class DeValidatedHistoricalDataMarketDocumentProvider implements ValidatedHistoricalDataMarketDocumentProvider {

    private final Flux<IdentifiableValidatedHistoricalData> identifiableData;
    private final PlainDeConfiguration deConfiguration;
    private final DataNeedsService dataNeedsService;

    public DeValidatedHistoricalDataMarketDocumentProvider(
            ValidatedHistoricalDataStream stream,
            PlainDeConfiguration deConfiguration,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService
    ) {
        this.identifiableData = stream.validatedHistoricalData();
        this.deConfiguration = deConfiguration;
        this.dataNeedsService = dataNeedsService;
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableData
                .map(this::toIntermediateDocument)
                .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVHD);
    }

    private IntermediateValidatedHistoricalDataMarketDocument toIntermediateDocument(
            IdentifiableValidatedHistoricalData data
    ) {
        return new IntermediateValidatedHistoricalDataMarketDocument(
                deConfiguration,
                data,
                dataNeedsService
        );
    }
}

