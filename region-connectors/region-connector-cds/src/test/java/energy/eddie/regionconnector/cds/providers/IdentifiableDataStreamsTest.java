package energy.eddie.regionconnector.cds.providers;

import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertSame;

class IdentifiableDataStreamsTest {

    @SuppressWarnings("resource")
    @Test
    void testPublish_publishesUsageSegments() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> segments = List.of();
        var streams = new IdentifiableDataStreams();

        // When
        streams.publish(pr, segments);

        // Then
        StepVerifier.create(streams.usageSegments())
                    .then(streams::close)
                    .assertNext(res -> assertAll(
                            () -> assertSame(segments, res.payload()),
                            () -> assertSame(pr, res.permissionRequest())
                    ))
                    .verifyComplete();
    }
}