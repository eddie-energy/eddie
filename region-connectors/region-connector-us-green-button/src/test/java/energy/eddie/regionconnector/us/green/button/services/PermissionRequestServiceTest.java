package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.permission.events.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.UsUnfulfillableEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private UsPermissionRequestRepository repository;
    @InjectMocks
    private PermissionRequestService permissionRequestService;
    @Captor
    private ArgumentCaptor<UsUnfulfillableEvent> eventCaptor;

    @Test
    void removeUnfulfillablePermissionRequests_emitsUnfulfillable() {
        // Given
        var meterReadings = List.of(new MeterReading("pid",
                                                     "mid",
                                                     ZonedDateTime.now(ZoneOffset.UTC),
                                                     PollingStatus.DATA_READY));
        var pr1 = getPermissionRequest(meterReadings, "pid1");
        var pr2 = getPermissionRequest(List.of(), "pid2");
        when(repository.findAllById(List.of("pid1", "pid2"))).thenReturn(List.of(pr1, pr2));

        // When
        permissionRequestService.removeUnfulfillablePermissionRequests(List.of("pid1", "pid2"));

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertAll(
                () -> assertEquals("pid2", res.permissionId()),
                () -> assertTrue(res.requiresExternalTermination())
        );
    }

    @Test
    void findActivePermissionRequests_returnsActivePermissionRequests() {
        // Given
        var permissionRequests = List.of(getPermissionRequest(List.of(), "pid"));
        when(repository.findActivePermissionRequests())
                .thenReturn(permissionRequests);

        // When
        var res = permissionRequestService.findActivePermissionRequests();

        // Then
        assertEquals(permissionRequests, res);
    }

    private static GreenButtonPermissionRequest getPermissionRequest(List<MeterReading> meterReadings, String pid) {
        var today = LocalDate.now(ZoneOffset.UTC);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        return new GreenButtonPermissionRequest(
                pid,
                "cid",
                "dnid",
                today,
                today,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now,
                "US",
                "company",
                null,
                null,
                meterReadings,
                "1111"
        );
    }
}