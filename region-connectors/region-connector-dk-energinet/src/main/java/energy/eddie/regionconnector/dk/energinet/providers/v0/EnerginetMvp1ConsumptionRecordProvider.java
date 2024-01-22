package energy.eddie.regionconnector.dk.energinet.providers.v0;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.utils.ConsumptionRecordMapper;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.util.concurrent.Flow;

@Component
public class EnerginetMvp1ConsumptionRecordProvider implements Mvp1ConsumptionRecordProvider {
    private final Flux<IdentifiableApiResponse> identifiableApiResponseFlux;

    public EnerginetMvp1ConsumptionRecordProvider(Flux<IdentifiableApiResponse> identifiableApiResponseFlux) {
        this.identifiableApiResponseFlux = identifiableApiResponseFlux;
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(identifiableApiResponseFlux
                .map(ConsumptionRecordMapper::timeSeriesToConsumptionRecord));
    }

    @Override
    public void close() {
        // complete signal is propagated from input Flux
    }
}
