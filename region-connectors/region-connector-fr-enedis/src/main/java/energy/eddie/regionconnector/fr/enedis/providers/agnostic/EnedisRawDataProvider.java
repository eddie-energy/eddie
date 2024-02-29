package energy.eddie.regionconnector.fr.enedis.providers.agnostic;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Flow;

@Component
@ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
public class EnedisRawDataProvider implements RawDataProvider {
    private final Flux<RawDataMessage> rawDataMessageFlux;

    public EnedisRawDataProvider(Flux<IdentifiableMeterReading> identifiableMeterReadingFlux) {
        this.rawDataMessageFlux = identifiableMeterReadingFlux.map(this::createRawDataMessage);
    }

    @Override
    public Flow.Publisher<RawDataMessage> getRawDataStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(rawDataMessageFlux);
    }

    private RawDataMessage createRawDataMessage(IdentifiableMeterReading meterReading) {
        FrEnedisPermissionRequest permissionRequest = meterReading.permissionRequest();
        return new RawDataMessage(permissionRequest.permissionId(), permissionRequest.connectionId(), permissionRequest.dataNeedId(),
                permissionRequest.dataSourceInformation(), ZonedDateTime.now(ZoneId.of("UTC")), meterReading.meterReading().toString());
    }

    @Override
    public void close() {
        // complete is emitted when identifiableMeterReadingFlux completes
    }
}