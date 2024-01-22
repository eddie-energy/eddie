package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.v0.process.model.PermissionRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Flow;

@Component
@ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
public class DatadisRawDataProvider implements RawDataProvider {
    private final Flux<IdentifiableMeteringData> meteringDataFlux;

    public DatadisRawDataProvider(Flux<IdentifiableMeteringData> meteringDataFlux) {
        this.meteringDataFlux = meteringDataFlux;
    }

    @Override
    public Flow.Publisher<RawDataMessage> getRawDataStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                meteringDataFlux
                        .map(this::createRawDataMessage));
    }

    private RawDataMessage createRawDataMessage(IdentifiableMeteringData meteringData) {
        PermissionRequest request = meteringData.permissionRequest();
        return new RawDataMessage(request.permissionId(), request.connectionId(), request.dataNeedId(),
                request.dataSourceInformation(), ZonedDateTime.now(ZoneId.of("UTC")),
                meteringData.meteringData().toString());
    }

    @Override
    public void close() {
        // complete is emitted when meteringDataFlux completes
    }
}