package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.UsUnfulfillableEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    @SuppressWarnings("unused")
    private EntityManager entityManager;
    @InjectMocks
    private PermissionRequestService permissionRequestService;
    @Captor
    private ArgumentCaptor<UsUnfulfillableEvent> eventCaptor;

    @Test
    void removeUnfulfillablePermissionRequest_emitsUnfulfillable() {
        // Given
        var pr = getPermissionRequest(List.of());
        when(repository.getByPermissionId("pid")).thenReturn(pr);

        // When
        permissionRequestService.removeUnfulfillablePermissionRequest("pid");

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertTrue(res.requiresExternalTermination())
        );
    }

    @Test
    void removeUnfulfillablePermissionRequest_doesNothingForFulfillablePermissionRequest() {
        // Given
        var meterReading = new MeterReading("pid", "muid", ZonedDateTime.now(ZoneOffset.UTC), PollingStatus.DATA_READY);
        var pr = getPermissionRequest(List.of(meterReading));
        when(repository.getByPermissionId("pid")).thenReturn(pr);

        // When
        permissionRequestService.removeUnfulfillablePermissionRequest("pid");

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void findActivePermissionRequests_returnsActivePermissionRequests() {
        // Given
        var permissionRequests = List.of(getPermissionRequest(List.of()));
        when(repository.findActivePermissionRequests())
                .thenReturn(permissionRequests);

        // When
        var res = permissionRequestService.findActivePermissionRequests();

        // Then
        assertEquals(permissionRequests, res);
    }

    private static GreenButtonPermissionRequest getPermissionRequest(List<MeterReading> meterReadings) {
        return new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                        .setLastMeterReadings(meterReadings)
                                                        .build();
    }
}