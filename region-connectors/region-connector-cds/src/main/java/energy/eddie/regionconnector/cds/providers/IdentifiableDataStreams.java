package energy.eddie.regionconnector.cds.providers;

import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

@Component
public class IdentifiableDataStreams implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableDataStreams.class);
    private final Sinks.Many<IdentifiableUsageSegments> usageSegmentSink = Sinks.many()
                                                                                .multicast()
                                                                                .onBackpressureBuffer();

    public void publish(
            CdsPermissionRequest permissionRequest,
            List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> usageSegments
    ) {
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .log("Publishing usage segments for permission request {}");
        usageSegmentSink.tryEmitNext(new IdentifiableUsageSegments(permissionRequest, usageSegments));
    }

    public Flux<IdentifiableUsageSegments> usageSegments() {
        return usageSegmentSink.asFlux();
    }

    @Override
    public void close() {
        usageSegmentSink.tryEmitComplete();
    }
}
