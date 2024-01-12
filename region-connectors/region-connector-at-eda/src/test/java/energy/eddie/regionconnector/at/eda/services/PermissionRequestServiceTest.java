package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class PermissionRequestServiceTest {

    @Autowired
    private PermissionRequestService permissionRequestService;
    @MockBean
    private PermissionRequestFactory permissionRequestFactory;
    @MockBean
    private AtPermissionRequestRepository permissionRequestRepository;
    @MockBean
    private ServletWebServerApplicationContext ignore;
    @MockBean
    private Supplier<Integer> alsoIgnore;

    @Test
    void findByPermissionId_shouldReturnPermissionRequest() {
        // Given
        String permissionId = "123";
        AtPermissionRequest atPermissionRequest = new SimplePermissionRequest(permissionId, "cid", "dnid", "cmRequestId", "convId", null);
        when(permissionRequestRepository.findByPermissionId(permissionId)).thenReturn(Optional.of(atPermissionRequest));
        when(permissionRequestFactory.create(atPermissionRequest)).thenReturn(atPermissionRequest);

        // When
        Optional<AtPermissionRequest> result = permissionRequestService.findByPermissionId(permissionId);

        // Then
        assertEquals(Optional.of(atPermissionRequest), result);
    }

    @Test
    void findByConversationIdOrCMRequestId_shouldReturnPermissionRequest() {
        // Given
        String conversationId = "cid";
        AtPermissionRequest atPermissionRequest = new SimplePermissionRequest("pid", "connectionId", "dnid", "cmRequestId", conversationId, null);
        when(permissionRequestRepository.findByConversationIdOrCMRequestId(eq(conversationId), any())).thenReturn(Optional.of(atPermissionRequest));
        when(permissionRequestFactory.create(atPermissionRequest)).thenReturn(atPermissionRequest);

        // When
        Optional<AtPermissionRequest> result = permissionRequestService.findByConversationIdOrCMRequestId(conversationId, "asdf");

        // Then
        assertEquals(Optional.of(atPermissionRequest), result);
    }

    @Test
    void findByMeteringPointIdAndDate_shouldReturnListOfPermissionRequests() {
        // Given
        String meteringPoint = "123";
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.meteringPointId()).thenReturn(Optional.of("meteringPointId"));
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "permissionId", "dataNeedId", ccmoRequest, null);
        when(permissionRequestRepository.findByMeteringPointIdAndDate(eq(meteringPoint), any())).thenReturn(List.of(permissionRequest));
        when(permissionRequestFactory.create(permissionRequest)).thenReturn(permissionRequest);

        // When
        List<AtPermissionRequest> result = permissionRequestService.findByMeteringPointIdAndDate(meteringPoint, ZonedDateTime.now(ZoneOffset.UTC).toLocalDate());

        // Then
        assertEquals(List.of(permissionRequest), result);
    }

    @Test
    void findConnectionStatusMessageById_shouldReturnConnectionStatusMessage() {
        // Given
        String permissionId = "123";
        PermissionRequestState state = new AtAcceptedPermissionRequestState(null);
        AtPermissionRequest atPermissionRequest = new SimplePermissionRequest(permissionId, "cid", "dnid", "cmRequestId", "convId", state);


        when(permissionRequestRepository.findByPermissionId(permissionId)).thenReturn(Optional.of(atPermissionRequest));

        // When
        Optional<ConnectionStatusMessage> result = permissionRequestService.findConnectionStatusMessageById(permissionId);

        // Then
        assertTrue(result.isPresent());
        ConnectionStatusMessage expectedMessage = new ConnectionStatusMessage(
                atPermissionRequest.connectionId(),
                atPermissionRequest.permissionId(),
                atPermissionRequest.dataNeedId(),
                atPermissionRequest.dataSourceInformation(),
                result.get().timestamp(),
                atPermissionRequest.state().status(),
                null
        );

        assertEquals(Optional.of(expectedMessage), result);
    }
}