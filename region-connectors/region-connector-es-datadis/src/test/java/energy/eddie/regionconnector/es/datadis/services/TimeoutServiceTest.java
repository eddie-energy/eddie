package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.AllowedGranularity;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeoutServiceTest {
    @Mock
    private Outbox outbox;
    @Spy
    private DatadisConfig config = new PlainDatadisConfiguration("name", "password", "bla", 24);
    @Mock
    private EsPermissionRequestRepository repository;
    @InjectMocks
    private TimeoutService timeoutService;
    @Captor
    private ArgumentCaptor<EsSimpleEvent> simpleCaptor;

    @Test
    void testTimeout_emitsStalePermissionRequests() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.findStalePermissionRequests(24))
                .thenReturn(List.of(
                        new DatadisPermissionRequest(
                                "pid",
                                "cid",
                                "dnid",
                                Granularity.PT1H,
                                "nif",
                                "mid",
                                now,
                                now,
                                null,
                                null,
                                null,
                                PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                null,
                                false,
                                ZonedDateTime.now(ZoneOffset.UTC),
                                AllowedGranularity.PT15M_OR_PT1H
                        )
                ));

        // When
        timeoutService.timeout();

        // Then
        verify(outbox).commit(simpleCaptor.capture());
        var res = simpleCaptor.getValue();
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals(PermissionProcessStatus.TIMED_OUT, res.status())
        );
    }
}