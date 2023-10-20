package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionRequestFactoryTest {
    @Test
    void testCreatePermissionRequest() {
        // Given
        EdaAdapter edaAdapterMock = mock(EdaAdapter.class);
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        AtPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(edaAdapterMock, permissionStateMessages, permissionRequestRepository);

        String connectionId = "connection123";
        String dataNeedId = "dataNeedId";
        CCMORequest ccmoRequest = mock(CCMORequest.class);

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(connectionId, dataNeedId, ccmoRequest);

        // Then
        assertNotNull(permissionRequest);
    }

    @Test
    void testCreatedPermissionRequest_isSaving() throws FutureStateException, PastStateException {
        // Given
        EdaAdapter edaAdapterMock = mock(EdaAdapter.class);
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        AtPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(edaAdapterMock, permissionStateMessages, permissionRequestRepository);

        String connectionId = "connection123";
        String dataNeedId = "dataNeedId";
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        AtPermissionRequest permissionRequest = permissionRequestFactory.create(connectionId, dataNeedId, ccmoRequest);
        permissionRequestRepository.removeByPermissionId(permissionRequest.permissionId());

        // When
        permissionRequest.validate();

        // Then
        Optional<AtPermissionRequest> res = permissionRequestRepository.findByConversationIdOrCMRequestId("messageId", "cmRequestId");
        assertEquals(permissionRequest, res.get());
    }

    @Test
    void testCreatedPermissionRequest_isMessaging() throws FutureStateException, PastStateException {
        // Given
        EdaAdapter edaAdapterMock = mock(EdaAdapter.class);
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        AtPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(edaAdapterMock, permissionStateMessages, permissionRequestRepository);

        String connectionId = "connection123";
        String dataNeedId = "dataNeedId";
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        AtPermissionRequest permissionRequest = permissionRequestFactory.create(connectionId, dataNeedId, ccmoRequest);

        // When
        permissionRequest.validate();
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .consumeNextWith(cr -> {
                })
                .assertNext(cr -> assertAll(
                        () -> assertEquals(PermissionProcessStatus.VALIDATED, cr.status()),
                        () -> assertEquals(permissionRequest.permissionId(), cr.permissionId()),
                        () -> assertEquals(permissionRequest.connectionId(), cr.connectionId())
                ))
                .verifyComplete();
    }
}