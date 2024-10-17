package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.MandateResponseModel;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

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
                DefaultFluviusPermissionRequestBuilder.create()
                        .status(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                        .build()
        ));
        when(fluviusApi.mandateFor("pid")).thenReturn(
                Mono.just(createMandateResponse("Approved"))
        );

        // When
        service.checkForAcceptance();

        // Then
        verify(fluviusApi).mandateFor(any());
        verify(outbox).commit(assertArg(event -> {
            assertEquals("pid", event.permissionId());
            assertEquals(PermissionProcessStatus.ACCEPTED, event.status());
        }));
    }

    @Test
    void checkForAcceptance_noAcceptedRequests_checkAcceptanceRequested() {
        // Given
        when(repository.findByStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)).thenReturn(List.of(
                DefaultFluviusPermissionRequestBuilder.create()
                        .status(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                        .build()
        ));
        when(fluviusApi.mandateFor("pid")).thenReturn(
                Mono.just(createMandateResponse("Requested"))
        );

        // When
        service.checkForAcceptance();

        // Then
        verify(fluviusApi).mandateFor(any());
        verify(outbox, never()).commit(any());
    }

    private GetMandateResponseModelApiDataResponse createMandateResponse(String status) {
        var mandate = new MandateResponseModel()
                .referenceNumber("pid")
                .status(status);
        return new GetMandateResponseModelApiDataResponse().data(new GetMandateResponseModel().mandates(List.of(mandate)));
    }
}