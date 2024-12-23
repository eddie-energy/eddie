package energy.eddie.regionconnector.be.fluvius.streams;

import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component
public class IdentifiableDataStreams implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableDataStreams.class);
    private final Sinks.Many<IdentifiableMeteringData> meteringDataSink = Sinks.many()
                                                                               .multicast()
                                                                               .onBackpressureBuffer();

    public void publish(
            FluviusPermissionRequest permissionRequest,
            GetEnergyResponseModelApiDataResponse payload
    ) {
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .log("Publishing metering data for permission request {}");
        meteringDataSink.emitNext(new IdentifiableMeteringData(permissionRequest, payload),
                                  Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1))
        );
    }

    public Flux<IdentifiableMeteringData> getMeteringData() {
        return meteringDataSink.asFlux();
    }

    @Override
    public void close() {
        meteringDataSink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }
}
