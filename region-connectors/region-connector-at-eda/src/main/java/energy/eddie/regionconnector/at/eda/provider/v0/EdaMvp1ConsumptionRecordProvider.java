package energy.eddie.regionconnector.at.eda.provider.v0;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.processing.mvp1.Mvp1ConsumptionRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EdaMvp1ConsumptionRecordProvider implements Mvp1ConsumptionRecordProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaMvp1ConsumptionRecordProvider.class);
    private final Mvp1ConsumptionRecordMapper mvp1ConsumptionRecordMapper;

    private final Flux<ConsumptionRecord> mvp1ConsumptionRecordFlux;

    public EdaMvp1ConsumptionRecordProvider(Mvp1ConsumptionRecordMapper mvp1ConsumptionRecordMapper, Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordStream) {
        this.mvp1ConsumptionRecordMapper = mvp1ConsumptionRecordMapper;

        this.mvp1ConsumptionRecordFlux = identifiableConsumptionRecordStream
                .flatMap(this::mapEdaConsumptionRecordToMvp1ConsumptionRecord);  // the mapping method is called for each element for each subscriber if we at some point have multiple subscribers, consider using publish().refCount()
    }

    @Override
    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return mvp1ConsumptionRecordFlux;
    }

    private Flux<ConsumptionRecord> mapEdaConsumptionRecordToMvp1ConsumptionRecord(IdentifiableConsumptionRecord identifiableConsumptionRecord) {
        try {
            var consumptionRecord = mvp1ConsumptionRecordMapper.mapToMvp1ConsumptionRecord(identifiableConsumptionRecord.consumptionRecord());
            return Flux.fromIterable(identifiableConsumptionRecord.permissionRequests())
                    .map(permissionRequest -> {
                        consumptionRecord.setPermissionId(permissionRequest.permissionId());
                        consumptionRecord.setConnectionId(permissionRequest.connectionId());
                        consumptionRecord.setDataNeedId(permissionRequest.dataNeedId());
                        return consumptionRecord;
                    });
        } catch (InvalidMappingException e) {
            LOGGER.error("Could not map EDA consumption record to MVP1 consumption record", e);
            return Flux.empty();
        }
    }

    @Override
    public void close() throws Exception {
        // Nothing to clean up, flux is closed when the underlying flux is closed
    }
}