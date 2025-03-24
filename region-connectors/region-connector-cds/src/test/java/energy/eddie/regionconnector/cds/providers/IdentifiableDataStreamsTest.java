package energy.eddie.regionconnector.cds.providers;

import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentifiableDataStreamsTest {

    @SuppressWarnings("resource")
    @Test
    void testPublish_publishesUsageSegments() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var streams = new IdentifiableDataStreams();

        // When
        streams.publish(pr, List.of(), List.of(), List.of(), List.of(), List.of());

        // Then
        StepVerifier.create(streams.validatedHistoricalData())
                    .then(streams::close)
                    .assertNext(res -> assertAll(
                            () -> assertEquals(List.of(), res.payload().usageSegments()),
                            () -> assertSame(pr, res.permissionRequest())
                    ))
                    .verifyComplete();
    }
}