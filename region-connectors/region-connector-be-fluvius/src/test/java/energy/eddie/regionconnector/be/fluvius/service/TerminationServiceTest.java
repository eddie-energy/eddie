package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminationServiceTest {
    @Mock
    private BePermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private TerminationService terminationService;

    @Test
    void testTerminate_unknownPermission_doesNothing() {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());

        // When
        terminationService.terminate("pid");

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testTerminate_notAcceptedPermission_doesNothing() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(
                        new FluviusPermissionRequest(
                                "pid",
                                "cid",
                                "dnid",
                                PermissionProcessStatus.CREATED,
                                Granularity.PT15M,
                                now,
                                now,
                                ZonedDateTime.now(ZoneOffset.UTC),
                                Flow.B2B
                        )
                ));

        // When
        terminationService.terminate("pid");

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testTerminate_acceptedPermission_emitsTerminated() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(
                        new FluviusPermissionRequest(
                                "pid",
                                "cid",
                                "dnid",
                                PermissionProcessStatus.ACCEPTED,
                                Granularity.PT15M,
                                now,
                                now,
                                ZonedDateTime.now(ZoneOffset.UTC),
                                Flow.B2B
                        )
                ));

        // When
        terminationService.terminate("pid");

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.TERMINATED, event.status())
        )));
    }
}