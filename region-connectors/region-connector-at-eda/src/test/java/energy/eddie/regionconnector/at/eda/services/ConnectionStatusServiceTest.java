package energy.eddie.regionconnector.at.eda.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionStatusServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModules(new JavaTimeModule(), new Jdk8Module());
    @Mock
    private AtPermissionRequestRepository permissionRequestRepository;

    @Test
    void findConnectionStatusMessageById_shouldReturnConnectionStatusMessage() {
        // Given
        var permissionRequestService = new ConnectionStatusService(permissionRequestRepository, objectMapper);
        String permissionId = "123";
        AtPermissionRequest atPermissionRequest = new SimplePermissionRequest(permissionId, "cid", "dnid",
                                                                              "cmRequestId", "convId",
                                                                              PermissionProcessStatus.ACCEPTED);
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
                atPermissionRequest.status(),
                null,
                objectMapper.createObjectNode().put("cmRequestId", atPermissionRequest.cmRequestId())
        );

        assertEquals(Optional.of(expectedMessage), result);
    }
}
