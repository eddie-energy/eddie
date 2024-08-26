package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrAcceptedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisDataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.providers.v0_82.SimpleFrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.services.AccountingPointDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {
    @Mock
    private AccountingPointDataService accountingPointDataService;
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @Mock
    private FrPermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedHandler acceptedHandler;

    @Test
    void testAccept_fetchesMeteringPointSegment() {
        // Given
        String pid = "pid";
        String usagePointId = "usagePointId";
        FrEnedisPermissionRequest permissionRequest = permissionRequest(Granularity.PT30M);
        when(repository.getByPermissionId(pid)).thenReturn(permissionRequest);

        // When
        eventBus.emit(new FrAcceptedEvent(pid, usagePointId));

        // Then
        verify(accountingPointDataService).fetchMeteringPointSegment(pid, usagePointId);
    }

    private static SimpleFrEnedisPermissionRequest permissionRequest(@Nullable Granularity granularity) {
        return new SimpleFrEnedisPermissionRequest(
                "usagePointId",
                granularity,
                UsagePointType.CONSUMPTION,
                Optional.empty(),
                "permissionId",
                "connectionId",
                "dataNeedId",
                PermissionProcessStatus.ACCEPTED,
                new EnedisDataSourceInformation(),
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC)
        );
    }

    @Test
    void testAccept_fetchAccountingPointData() {
        // Given
        String pid = "pid";
        String usagePointId = "usagePointId";
        FrEnedisPermissionRequest permissionRequest = permissionRequest(null);
        when(repository.getByPermissionId(pid)).thenReturn(permissionRequest);

        // When
        eventBus.emit(new FrAcceptedEvent(pid, usagePointId));

        // Then
        verify(accountingPointDataService).fetchAccountingPointData(permissionRequest, usagePointId);
    }
}
