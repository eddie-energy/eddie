package energy.eddie.regionconnector.es.datadis.providers.v0;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import energy.eddie.regionconnector.es.datadis.ConsumptionRecordMapper;
import energy.eddie.regionconnector.es.datadis.InvalidMappingException;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.function.Predicate;

@Component
public class DatadisMvp1ConsumptionRecordProvider implements Mvp1ConsumptionRecordProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatadisMvp1ConsumptionRecordProvider.class);
    private final Flux<IdentifiableMeteringData> meteringDataFlux;

    public DatadisMvp1ConsumptionRecordProvider(Flux<IdentifiableMeteringData> meteringDataFlux) {
        this.meteringDataFlux = meteringDataFlux;
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                meteringDataFlux
                        .mapNotNull(this::createMvp1ConsumptionRecord));
    }

    private @Nullable ConsumptionRecord createMvp1ConsumptionRecord(IdentifiableMeteringData identifiableMeteringData) {
        var permissionRequest = identifiableMeteringData.permissionRequest();
        var meteringData = identifiableMeteringData.meteringData();

        var from = permissionRequest.requestDataFrom().toLocalDate();
        var to = permissionRequest.requestDataTo().toLocalDate();

        // remove metering data that is not in the requested time range
        meteringData.removeIf(notInRequestedRange(from, to));
        permissionRequest.setLastPulledMeterReading(Objects.requireNonNull(meteringData.getLast().date()).atStartOfDay(ZoneOffset.UTC));

        try {
            return ConsumptionRecordMapper.mapToMvp1ConsumptionRecord(
                    meteringData,
                    permissionRequest.permissionId(),
                    permissionRequest.connectionId(),
                    permissionRequest.measurementType(),
                    permissionRequest.dataNeedId()
            );
        } catch (InvalidMappingException e) {
            LOGGER.error("Invalid mapping exception while mapping for raw data output.", e);
            return null;
        }
    }

    private static Predicate<MeteringData> notInRequestedRange(LocalDate from, LocalDate to) {
        return m -> m.date() == null || m.date().isBefore(from) || m.date().isAfter(to);
    }

    @Override
    public void close() {
        // complete is emitted when meteringDataFlux completes
    }
}
