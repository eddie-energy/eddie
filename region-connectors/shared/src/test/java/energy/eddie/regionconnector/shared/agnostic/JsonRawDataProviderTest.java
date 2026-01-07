package energy.eddie.regionconnector.shared.agnostic;

import tools.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonRawDataProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @Test
    void rawDataProviderPublishesRawDataMessages() {
        // Given
        TestPublisher<IdentifiablePayload<PermissionRequest, String>> publisher1 = TestPublisher.create();
        TestPublisher<IdentifiablePayload<PermissionRequest, String>> publisher2 = TestPublisher.create();
        //noinspection ReactiveStreamsUnusedPublisher
        var provider = new JsonRawDataProvider("at-eda", objectMapper, publisher1.flux(), publisher2.flux());
        var pr = new SimplePermissionRequest("pid", "cid", "dnid", PermissionProcessStatus.ACCEPTED);
        var i1 = new IdentifiableStringPayload(pr, "first");
        var i2 = new IdentifiableStringPayload(pr, "second");
        var i3 = new IdentifiableStringPayload(pr, "third");

        // Then
        StepVerifier.create(provider.getRawDataStream())
                    .then(() -> {
                        publisher1.next(i1);
                        publisher2.next(i2);
                        publisher1.next(i3);
                        publisher1.complete();
                        publisher2.complete();
                    })
                    // When
                    .assertNext(res -> assertEquals("\"" + i1.payload() + "\"", res.rawPayload()))
                    .assertNext(res -> assertEquals("\"" + i2.payload() + "\"", res.rawPayload()))
                    .assertNext(res -> assertEquals("\"" + i3.payload() + "\"", res.rawPayload()))
                    .verifyComplete();

        // Clean-Up
        provider.close();
    }

    private record IdentifiableStringPayload(PermissionRequest permissionRequest,
                                             String payload) implements IdentifiablePayload<PermissionRequest, String> {}
}