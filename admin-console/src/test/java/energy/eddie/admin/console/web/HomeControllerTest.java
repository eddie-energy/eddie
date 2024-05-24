package energy.eddie.admin.console.web;

import energy.eddie.admin.console.data.StatusMessage;
import energy.eddie.admin.console.data.StatusMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private StatusMessageRepository statusMessageRepository;

    @Mock
    private Model model;

    private HomeController homeController;

    @BeforeEach
    public void setup() {
        homeController = new HomeController(statusMessageRepository);
    }

    @Test
    void testHome() {
        StatusMessage statusMessage = new StatusMessage("testPermissionId", "testCountry", "testDso", "2024-05-22T08:20:03+02:00", "A05");

        when(statusMessageRepository.findLatestStatusMessageForAllPermissions()).thenReturn(Collections.singletonList(statusMessage));

        String viewName = homeController.home(model);

        verify(statusMessageRepository, times(1)).findLatestStatusMessageForAllPermissions();
        verify(model, times(1)).addAttribute(eq("title"), anyString());
        verify(model, times(1)).addAttribute(eq("statusMessages"), anyList());
        verify(model, times(1)).addAttribute(eq("statusDisplays"), anyList());
        verify(model, times(1)).addAttribute(eq("nonTerminatableStatuses"), anyList());

        assertEquals("index", viewName);
    }

    @Test
    void testStatusDisplays() {
        // Given
        StatusMessage unknownStatusMessage = new StatusMessage("testPermissionId", "testCountry", "testDso", "2024-05-22T08:20:03+02:00", "ABCDEF");
        when(statusMessageRepository.findLatestStatusMessageForAllPermissions()).thenReturn(Collections.singletonList(unknownStatusMessage));

        // When
        String viewName = homeController.home(model);
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(model, times(1)).addAttribute(eq("statusDisplays"), captor.capture());
        List<String> statusDisplays = captor.getValue();

        // Then
        assertEquals(1, statusDisplays.size());
        assertEquals("UNKNOWN_STATUS", statusDisplays.getFirst());
    }
}