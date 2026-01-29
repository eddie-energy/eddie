package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.de.eta.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.de.eta.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.de.eta.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.MalformedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestCreationServiceTest {

    private static final String CONNECTION_ID = "conn-1";

    @Mock
    private DataNeedCalculationService<energy.eddie.dataneeds.needs.DataNeed> dataNeedCalculationService;

    @Mock
    private Outbox outbox;

    private PermissionRequestCreationService service;

    @BeforeEach
    void setUp() {
        service = new PermissionRequestCreationService(dataNeedCalculationService, outbox);
    }

    @Test
    void createPermissionRequestWhenDataNeedIsValidatedShouldReturnCreatedPermissionRequest() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        PermissionRequestForCreation request = new PermissionRequestForCreation(CONNECTION_ID, "dn-1", "mp-1");
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = LocalDate.now(ZoneId.systemDefault()).plusDays(30);
        Timeframe timeframe = new Timeframe(start, end);
        
        ValidatedHistoricalDataDataNeedResult result = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT15M),
                timeframe,
                timeframe
        );
        
        when(dataNeedCalculationService.calculate(anyString())).thenReturn(result);

        var response = service.createPermissionRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.permissionId()).isNotNull();
        
        verify(outbox).commit(any(CreatedEvent.class));
        verify(outbox).commit(any(ValidatedEvent.class));
    }

    @Test
    void createPermissionRequestWhenAccountingPointDataNeedShouldSucceedAndCommitValidated() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        PermissionRequestForCreation request = new PermissionRequestForCreation(CONNECTION_ID, "dn-1", "mp-1");
        Timeframe timeframe = new Timeframe(LocalDate.now(ZoneId.systemDefault()), LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        when(dataNeedCalculationService.calculate(anyString())).thenReturn(new AccountingPointDataNeedResult(timeframe));

        CreatedPermissionRequest result = service.createPermissionRequest(request);

        assertThat(result).isNotNull();
        assertThat(result.permissionId()).isNotNull();
        verify(outbox).commit(any(CreatedEvent.class));
        verify(outbox).commit(any(ValidatedEvent.class));
        verify(outbox, never()).commit(any(MalformedEvent.class));
    }

    @Test
    void createPermissionRequestWhenDataNeedNotFoundShouldThrowNotFoundAndCommitMalformed() {
        PermissionRequestForCreation request = new PermissionRequestForCreation(CONNECTION_ID, "dn-1", "mp-1");
        when(dataNeedCalculationService.calculate(anyString())).thenReturn(new DataNeedNotFoundResult());

        assertThatThrownBy(() -> service.createPermissionRequest(request))
                .isInstanceOf(DataNeedNotFoundException.class);

        verify(outbox).commit(any(CreatedEvent.class));
        verify(outbox).commit(any(MalformedEvent.class));
    }

    @Test
    void createPermissionRequestWhenDataNeedNotSupportedShouldThrowUnsupportedAndCommitMalformed() {
        PermissionRequestForCreation request = new PermissionRequestForCreation(CONNECTION_ID, "dn-1", "mp-1");
        when(dataNeedCalculationService.calculate(anyString())).thenReturn(new DataNeedNotSupportedResult("Unsupported"));

        assertThatThrownBy(() -> service.createPermissionRequest(request))
                .isInstanceOf(UnsupportedDataNeedException.class);

        verify(outbox).commit(any(CreatedEvent.class));
        verify(outbox).commit(any(MalformedEvent.class));
    }
}
