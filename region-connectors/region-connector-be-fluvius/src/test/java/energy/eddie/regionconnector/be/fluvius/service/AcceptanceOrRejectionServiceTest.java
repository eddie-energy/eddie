// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.mandate.GetMandateResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.mandate.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.mandate.MandateResponseModel;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptanceOrRejectionServiceTest {
    @Mock
    private FluviusApi fluviusApi;
    @Mock
    private Outbox outbox;
    @Mock
    private BePermissionRequestRepository repository;
    @Mock
    private DataNeedsService dataNeedsService;
    @InjectMocks
    private AcceptanceOrRejectionService service;
    @Captor
    private ArgumentCaptor<PermissionEvent> eventCaptor;

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
        when(repository.findByStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR))
                .thenReturn(List.of(DefaultFluviusPermissionRequestBuilder.create()
                                                                          .status(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                                                                          .dataNeedId("dnid")
                                                                          .build()
                ));
        when(fluviusApi.mandateFor("pid")).thenReturn(
                Mono.just(createMandateResponse(MandateResponseModel.Status.APPROVED))
        );
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(new RelativeDuration(null,
                                                                                     null,
                                                                                     null),
                                                                EnergyType.ELECTRICITY,
                                                                Granularity.PT15M,
                                                                Granularity.P1D));

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
                Mono.just(createMandateResponse(MandateResponseModel.Status.REQUESTED))
        );

        // When
        service.checkForAcceptance();

        // Then
        verify(fluviusApi).mandateFor(any());
        verify(outbox, never()).commit(any());
    }

    @Test
    void testAcceptOrRejectPermissionRequest_throwsOnInvalidStatus() {
        // Given
        // When & Then
        assertThrows(
                IllegalArgumentException.class,
                () -> service.acceptOrRejectPermissionRequest(
                        "pid",
                        PermissionProcessStatus.VALIDATED
                )
        );
    }

    @Test
    void testAcceptOrRejectPermissionRequest_throwsOnUnknownPermissionId() {
        // Given
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                PermissionNotFoundException.class,
                () -> service.acceptOrRejectPermissionRequest(
                        "pid",
                        PermissionProcessStatus.ACCEPTED
                )
        );
    }

    @Test
    void testAcceptOrRejectPermissionRequest_returnsAcceptOnAlreadyAcceptedPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(PermissionProcessStatus.ACCEPTED)));

        // When
        var res = service.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.ACCEPTED);

        // Then
        assertTrue(res);
    }

    @Test
    void testAcceptOrRejectPermissionRequest_returnsRejectOnAlreadyRejectedPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(PermissionProcessStatus.REJECTED)));

        // When
        var res = service.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.REJECTED);

        // Then
        assertFalse(res);
    }

    @Test
    void testAcceptOrRejectPermissionRequest_returnsAcceptForAcceptedPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)));
        var publisher = TestPublisher.<GetMandateResponseModelApiDataResponse>create();
        when(fluviusApi.mandateFor("pid"))
                .thenReturn(publisher.mono());
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(new RelativeDuration(null,
                                                                                     null,
                                                                                     null),
                                                                EnergyType.ELECTRICITY,
                                                                Granularity.PT15M,
                                                                Granularity.P1D));

        // When
        var res = service.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.ACCEPTED);

        // Then
        assertTrue(res);
        StepVerifier.create(publisher)
                    .then(() -> {
                        publisher.emit(createMandateResponse(MandateResponseModel.Status.APPROVED));
                        publisher.complete();
                    })
                    .expectNextCount(1)
                    .then(() -> verify(outbox)
                            .commit(assertArg(e -> assertEquals(PermissionProcessStatus.ACCEPTED, e.status()))))
                    .verifyComplete();
    }

    @Test
    void testAcceptOrRejectPermissionRequest_emitsUnfulfillableWhenNoMeterProvidesCorrectEnergyTypeOrGranularity() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)));
        var publisher = TestPublisher.<GetMandateResponseModelApiDataResponse>create();
        when(fluviusApi.mandateFor("pid"))
                .thenReturn(publisher.mono());
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(new RelativeDuration(null,
                                                                                     null,
                                                                                     null),
                                                                EnergyType.NATURAL_GAS,
                                                                Granularity.PT15M,
                                                                Granularity.PT1H));
        var mandate1 = new MandateResponseModel("pid",
                                                MandateResponseModel.Status.APPROVED,
                                                null,
                                                energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType.ELECTRICITY,
                                                null,
                                                null,
                                                MandateResponseModel.DataServiceType.HOURLY_OR_QUARTER_HOURLY,
                                                null,
                                                null);
        var mandate2 = new MandateResponseModel("pid",
                                                MandateResponseModel.Status.APPROVED,
                                                null,
                                                energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType.GAS,
                                                null,
                                                null,
                                                MandateResponseModel.DataServiceType.UNDEFINED,
                                                null,
                                                null);
        var data = new GetMandateResponseModelApiDataResponse(null,
                                                              new GetMandateResponseModel(null,
                                                                                          List.of(mandate1, mandate2)));

        // When
        var res = service.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.ACCEPTED);

        // Then
        assertTrue(res);
        StepVerifier.create(publisher)
                    .then(() -> {
                        publisher.emit(data);
                        publisher.complete();
                    })
                    .expectNextCount(1)
                    .verifyComplete();
        verify(outbox, times(2)).commit(eventCaptor.capture());
        assertEquals(PermissionProcessStatus.ACCEPTED, eventCaptor.getAllValues().getFirst().status());
        assertEquals(PermissionProcessStatus.UNFULFILLABLE, eventCaptor.getValue().status());
    }

    @Test
    void testAcceptOrRejectPermissionRequest_returnsRejectForRejectedPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)));

        // When
        var res = service.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.REJECTED);

        // Then
        assertFalse(res);
        verify(outbox).commit(assertArg(e -> assertEquals(PermissionProcessStatus.REJECTED, e.status())));
    }

    @Test
    void testAcceptOrRejectPermissionRequest_rejectsPermissionRequest_ifAllMetersHaveBeenRejected() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)));
        when(fluviusApi.mandateFor("pid"))
                .thenReturn(Mono.just(createMandateResponse(MandateResponseModel.Status.REJECTED)));
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(new RelativeDuration(null,
                                                                                     null,
                                                                                     null),
                                                                EnergyType.ELECTRICITY,
                                                                Granularity.PT15M,
                                                                Granularity.P1D));

        // When
        service.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.ACCEPTED);

        // Then
        verify(outbox).commit(assertArg(e -> assertEquals(PermissionProcessStatus.REJECTED, e.status())));
    }

    @Test
    void testAcceptOrRejectPermissionRequest_withoutData_doesNothing() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)));
        when(fluviusApi.mandateFor("pid"))
                .thenReturn(Mono.just(new GetMandateResponseModelApiDataResponse(null, null)));

        // When
        service.acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.ACCEPTED);

        // Then
        verify(outbox, never()).commit(any());
    }

    private static FluviusPermissionRequest getPermissionRequest(PermissionProcessStatus status) {
        return new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .status(status)
                .dataNeedId("dnid")
                .granularity(Granularity.PT15M)
                .build();
    }

    private GetMandateResponseModelApiDataResponse createMandateResponse(MandateResponseModel.Status status) {
        var mandate = new MandateResponseModel("pid",
                                               status,
                                               null,
                                               energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType.ELECTRICITY,
                                               null,
                                               null,
                                               MandateResponseModel.DataServiceType.HOURLY_OR_QUARTER_HOURLY,
                                               null,
                                               null);
        return new GetMandateResponseModelApiDataResponse(null, new GetMandateResponseModel(null, List.of(mandate)));
    }
}