package energy.eddie.outbound.admin.console.web;

import energy.eddie.outbound.admin.console.data.StatusMessage;
import energy.eddie.outbound.admin.console.data.StatusMessageDTO;
import energy.eddie.outbound.admin.console.data.StatusMessageRepository;
import energy.eddie.outbound.admin.console.services.TerminationAdminConsoleConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private StatusMessageRepository statusMessageRepository;
    @Mock
    private TerminationAdminConsoleConnector terminationConnector;
    @InjectMocks
    private HomeController homeController;

    @Test
    void testGetStatusMessages() {
        // Given
        StatusMessage statusMessage1 = new StatusMessage("testPermissionId", "testRegionConnectorId", "testDataNeedId", "testCountry", "testDso", "2024-05-22T08:20:03+02:00", "A06", "ACCEPTED");
        StatusMessage statusMessage2 = new StatusMessage("testPermissionId", "testRegionConnectorId", "testDataNeedId", "testCountry", "testDso", "2024-05-22T08:20:03+02:00", "A05", "ACCEPTED");
        List<StatusMessage> statusMessages = Arrays.asList(statusMessage1, statusMessage2);

        when(statusMessageRepository.findByPermissionIdOrderByStartDateDescIdDesc("testPermissionId")).thenReturn(statusMessages);

        // When
        ResponseEntity<List<StatusMessageDTO>> response = homeController.getStatusMessages("testPermissionId");

        // Then
        verify(statusMessageRepository, times(1)).findByPermissionIdOrderByStartDateDescIdDesc("testPermissionId");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        assertEquals("Available", response.getBody().get(0).cimStatus());
        assertEquals("Active", response.getBody().get(1).cimStatus());
    }

    @Test
    void testTerminatePermission() {
        // Given
        String permissionId = "testPermissionId";
        StatusMessage testStatusMessage = new StatusMessage(permissionId, "testCountry", "testRegionConnectorId", "testDataNeedId", "testDso", "2024-05-22T08:20:03+02:00", "A05", "ACCEPTED");
        when(statusMessageRepository.findByPermissionIdOrderByStartDateDescIdDesc(permissionId)).thenReturn(Collections.singletonList(testStatusMessage));

        // When
        ResponseEntity<Void> response = homeController.terminatePermission(permissionId);

        // Then
        ArgumentCaptor<String> permissionIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> regionConnectorIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(terminationConnector, times(1)).terminate(permissionIdCaptor.capture(), regionConnectorIdCaptor.capture());
        assertEquals(ResponseEntity.ok().build(), response);
    }
}