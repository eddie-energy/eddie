package energy.eddie.outbound.admin.console.web;

import energy.eddie.outbound.admin.console.data.StatusMessage;
import energy.eddie.outbound.admin.console.data.StatusMessageDTO;
import energy.eddie.outbound.admin.console.data.StatusMessageRepository;
import energy.eddie.outbound.admin.console.services.TerminationAdminConsoleConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

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
    @Mock
    private Model model;
    @Captor
    private ArgumentCaptor<List<StatusMessageDTO>> captor;
    @InjectMocks
    private HomeController homeController;

    @Test
    void testHome() {
        StatusMessage statusMessage = new StatusMessage("testPermissionId", "testRegionConnectorId", "testDataNeedId", "testCountry", "testDso", "2024-05-22T08:20:03+02:00", "A05");

        when(statusMessageRepository.findLatestStatusMessageForAllPermissions()).thenReturn(Collections.singletonList(statusMessage));

        String viewName = homeController.home(model, "false");

        verify(statusMessageRepository, times(1)).findLatestStatusMessageForAllPermissions();
        verify(model, times(1)).addAttribute(eq("title"), anyString());
        verify(model, times(1)).addAttribute(eq("statusMessages"), anyList());
        verify(model, times(1)).addAttribute(eq("nonTerminatableStatuses"), anyList());

        assertEquals("index", viewName);
    }

    @Test
    void testStatusDisplays() {
        // Given
        StatusMessage unknownStatusMessage = new StatusMessage("testPermissionId", "testRegionConnectorId", "testDataNeedId", "testCountry", "testDso", "2024-05-22T08:20:03+02:00", "ABCDEF");
        when(statusMessageRepository.findLatestStatusMessageForAllPermissions()).thenReturn(Collections.singletonList(unknownStatusMessage));

        // When
        homeController.home(model, "false");
        verify(model, times(1)).addAttribute(eq("statusMessages"), captor.capture());
        List<StatusMessageDTO> statusMessages = captor.getValue();

        // Then
        assertEquals(1, statusMessages.size());
        assertEquals("UNKNOWN_STATUS", statusMessages.getFirst().status());
    }

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
        assertEquals("Available", response.getBody().get(0).status());
        assertEquals("Active", response.getBody().get(1).status());
    }

    @Test
    void testCountryCodePrefixRemoval() {
        // Given
        StatusMessage statusMessageWithPrefix = new StatusMessage("testPermissionId", "testRegionConnectorId", "testDataNeedId", "NCountry", "testDso", "2024-05-22T08:20:03+02:00", "A05");
        when(statusMessageRepository.findLatestStatusMessageForAllPermissions()).thenReturn(Collections.singletonList(statusMessageWithPrefix));

        // When
        homeController.home(model, "false");
        verify(model, times(1)).addAttribute(eq("statusMessages"), captor.capture());
        List<StatusMessageDTO> statusMessages = captor.getValue();

        // Then
        assertEquals(1, statusMessages.size());
        assertEquals("Country", statusMessages.getFirst().country());
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