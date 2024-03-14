package energy.eddie.regionconnector.es.datadis.providers.v0;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import energy.eddie.regionconnector.es.datadis.ConsumptionRecordMapper;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DatadisMvp1ConsumptionRecordProvider implements Mvp1ConsumptionRecordProvider {
    private final Flux<IdentifiableMeteringData> meteringDataFlux;

    public DatadisMvp1ConsumptionRecordProvider(Flux<IdentifiableMeteringData> meteringDataFlux) {
        this.meteringDataFlux = meteringDataFlux;
    }

    @Override
    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return meteringDataFlux
                .mapNotNull(this::createMvp1ConsumptionRecord);
    }

    private ConsumptionRecord createMvp1ConsumptionRecord(IdentifiableMeteringData identifiableMeteringData) {
        var permissionRequest = identifiableMeteringData.permissionRequest();

        return ConsumptionRecordMapper.mapToMvp1ConsumptionRecord(
                identifiableMeteringData.intermediateMeteringData(),
                permissionRequest.permissionId(),
                permissionRequest.connectionId(),
                permissionRequest.measurementType(),
                permissionRequest.dataNeedId()
        );
    }

    @Override
    public void close() {
        // complete is emitted when meteringDataFlux completes
    }
}
