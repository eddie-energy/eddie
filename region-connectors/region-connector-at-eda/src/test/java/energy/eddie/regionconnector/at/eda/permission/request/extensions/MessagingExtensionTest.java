package energy.eddie.regionconnector.at.eda.permission.request.extensions;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.ponton.NoOpEdaAdapter;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest
class MessagingExtensionTest {

    @Autowired
    Sinks.Many<ConnectionStatusMessage> sink;
    // Stop Spring from trying to construct these beans
    @MockBean
    private ServletWebServerApplicationContext ignored;
    @MockBean
    private Supplier<Integer> alsoIgnored;

    @Test
    void extension_sendsMessage() {
        // Given
        MessagingExtension messagingExtension = new MessagingExtension(sink);
        EdaPermissionRequest permissionRequest = new EdaPermissionRequest("cid", "dnid", mock(CCMORequest.class), new NoOpEdaAdapter());

        // When
        messagingExtension.accept(permissionRequest);

        // Then
        StepVerifier.create(sink.asFlux())
                .then(() -> sink.tryEmitComplete())
                .assertNext(csm ->
                        assertAll(
                                () -> assertEquals(PermissionProcessStatus.CREATED, csm.status()),
                                () -> assertEquals(permissionRequest.permissionId(), csm.permissionId())
                        )
                )
                .expectComplete()
                .verify();
    }

}