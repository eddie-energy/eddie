package energy.eddie.regionconnector.de.eta.providers.v0_82;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.config.PlainDeConfiguration;
import energy.eddie.regionconnector.de.eta.dtos.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.streams.ValidatedHistoricalDataStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Provider for CIM v0.82 Validated Historical Data Envelopes.
 * Transforms validated historical data from the ETA Plus API into CIM v0.82 format.
 * This provider maintains backwards compatibility with the older CIM version.
 */
@Component
public class DeValidatedHistoricalDataEnvelopeProvider implements ValidatedHistoricalDataEnvelopeProvider {

    private final Flux<IdentifiableValidatedHistoricalData> identifiableData;
    private final PlainDeConfiguration deConfiguration;
    private final DataNeedsService dataNeedsService;

    public DeValidatedHistoricalDataEnvelopeProvider(
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
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableData
                .map(this::toIntermediateDocument)
                .flatMapIterable(IntermediateValidatedHistoricalDataEnvelope::toVHD);
    }

    private IntermediateValidatedHistoricalDataEnvelope toIntermediateDocument(
            IdentifiableValidatedHistoricalData data
    ) {
        return new IntermediateValidatedHistoricalDataEnvelope(
                deConfiguration,
                data,
                dataNeedsService
        );
    }

    @Override
    public void close() throws Exception {
        // No resources to clean up
    }
}

