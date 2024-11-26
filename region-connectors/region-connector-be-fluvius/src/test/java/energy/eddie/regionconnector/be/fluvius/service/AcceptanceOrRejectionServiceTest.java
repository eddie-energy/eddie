package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.MandateResponseModel;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptanceOrRejectionServiceTest {
    @Mock
    private FluviusApi fluviusApi;
    @Mock
    private Outbox outbox;
    @Mock
    private BePermissionRequestRepository repository;
    @InjectMocks
    private AcceptanceOrRejectionService service;

    @Test
    void checkForAcceptance_noAcceptedRequests_noRequests() {
        // Given
        when(repository.findByStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)).thenReturn(
                List.of()
        );

        // When
        service.checkForAcceptance();

        // Then
        verify(fluviusApi, never()).mandateFor(any());
    }


    @Test
    void checkForAcceptance_noAcceptedRequests_checkAcceptanceApproved() {
        // Given
        when(repository.findByStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)).thenReturn(List.of(
                createPermissionRequest()
        ));
        when(fluviusApi.mandateFor("pid1")).thenReturn(
                Mono.just(createMandateResponse("Approved"))
        );

        // When
        service.checkForAcceptance();

        // Then
        verify(fluviusApi).mandateFor(any());
        verify(outbox).commit(assertArg(event -> {
            assertEquals("pid1", event.permissionId());
            assertEquals(PermissionProcessStatus.ACCEPTED, event.status());
        }));
    }

    @Test
    void checkForAcceptance_noAcceptedRequests_checkAcceptanceRequested() {
        // Given
        when(repository.findByStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)).thenReturn(List.of(
                createPermissionRequest()
        ));
        when(fluviusApi.mandateFor("pid1")).thenReturn(
                Mono.just(createMandateResponse("Requested"))
        );

        // When
        service.checkForAcceptance();

        // Then
        verify(fluviusApi).mandateFor(any());
        verify(outbox, never()).commit(any());
    }

    private FluviusPermissionRequest createPermissionRequest() {
        return new FluviusPermissionRequest(
                "pid1",
                "cid1",
                "did1",
                PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                Granularity.PT15M,
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                ZonedDateTime.now(ZoneOffset.UTC),
                Flow.B2C
        );
    }

    private GetMandateResponseModelApiDataResponse createMandateResponse(String status) {
        var mandate = new MandateResponseModel()
                .referenceNumber("pid1")
                .status(status);
        return new GetMandateResponseModelApiDataResponse().data(new GetMandateResponseModel().mandates(List.of(mandate)));
    }
}