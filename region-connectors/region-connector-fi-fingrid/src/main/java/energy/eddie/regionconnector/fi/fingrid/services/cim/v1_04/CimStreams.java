package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04;

import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("CimStreamsV1_04")
public class CimStreams implements ValidatedHistoricalDataMarketDocumentProvider {
    private final EnergyDataService energyDataService;

    public CimStreams(EnergyDataService energyDataService) {
        this.energyDataService = energyDataService;
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return energyDataService.getIdentifiableValidatedHistoricalDataStream()
                                .flatMapIterable(id -> new IntermediateValidatedHistoricalDataMarketDocument(id.payload(),
                                                                                                             id.permissionRequest()).toVhds());
    }
}
