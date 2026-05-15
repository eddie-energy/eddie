package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.auth.AuthTokenResponse;
import energy.eddie.regionconnector.de.eta.auth.EtaAuthService;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.AuthenticationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.DeserializationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusBadRequestException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusForbiddenException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusNotFoundException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusServerException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusTimeoutException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.providers.AccountingPointDataStream;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusAccountingPointData;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountingPointDataHandlerTest {

    private static final String PID = "perm-ap-1";
    private static final String MPID = "malo-1";
    private static final String DATA_NEED_ID = "dn-1";
    private static final String ACCESS_TOKEN = "initial-access";
    private static final String REFRESH_TOKEN = "initial-refresh";

    private DePermissionRequestRepository repository;
    private DataNeedsService dataNeedsService;
    private EtaPlusApiClient apiClient;
    private EtaAuthService authService;
    private AccountingPointDataStream stream;
    private Outbox outbox;
    private EventBus eventBus;
    private AccountingPointDataNeed apDataNeed;
    private DePermissionRequest pr;

    @BeforeEach
    void setUp() {
        repository = mock(DePermissionRequestRepository.class);
        dataNeedsService = mock(DataNeedsService.class);
        apiClient = mock(EtaPlusApiClient.class);
        authService = mock(EtaAuthService.class);
        stream = mock(AccountingPointDataStream.class);
        outbox = mock(Outbox.class);
        eventBus = new EventBusImpl();
        apDataNeed = mock(AccountingPointDataNeed.class);

        pr = new DePermissionRequestBuilder()
                .permissionId(PID)
                .meteringPointId(MPID)
                .dataNeedId(DATA_NEED_ID)
                .accessToken(ACCESS_TOKEN)
                .refreshToken(REFRESH_TOKEN)
                .build();

        // Construct handler so it subscribes to the event bus
        new AccountingPointDataHandler(
                eventBus, repository, dataNeedsService, apiClient, authService, stream, outbox);
    }

    private void emit(AcceptedEvent event) {
        eventBus.emit(event);
    }

    private EtaPlusAccountingPointData samplePayload() {
        return new EtaPlusAccountingPointData(MPID, "42", "ELECTRICITY", "Consumption", null, null);
    }

    @Test
    void nonApDataNeed_skipsFetch() {
        when(repository.findByPermissionId(PID)).thenReturn(Optional.of(pr));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(mock(ValidatedHistoricalDataDataNeed.class));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        verifyNoInteractions(apiClient);
        verifyNoInteractions(stream);
        verifyNoInteractions(outbox);
    }

    @Test
    void missingPermissionRequest_logsAndReturns() {
        when(repository.findByPermissionId(PID)).thenReturn(Optional.empty());

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        verifyNoInteractions(apiClient);
        verifyNoInteractions(stream);
        verifyNoInteractions(outbox);
    }

    @Test
    void apDataNeed_200_publishesAndCommitsFulfilled() {
        when(repository.findByPermissionId(PID)).thenReturn(Optional.of(pr));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(apDataNeed);
        EtaPlusAccountingPointData payload = samplePayload();
        when(apiClient.fetchAccountingPointData(pr, ACCESS_TOKEN)).thenReturn(Mono.just(payload));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        verify(stream).publish(pr, payload);
        assertCommitted(PermissionProcessStatus.FULFILLED);
        verifyNoInteractions(authService);
    }

    @Test
    void apDataNeed_403_marksUnfulfillable() {
        primeFetchFailure(new EtaPlusForbiddenException("403", 403, new RuntimeException()));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        assertNoStreamPublish();
        assertCommitted(PermissionProcessStatus.UNFULFILLABLE);
    }

    @Test
    void apDataNeed_404_marksUnfulfillable() {
        primeFetchFailure(new EtaPlusNotFoundException("404", 404, new RuntimeException()));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        assertNoStreamPublish();
        assertCommitted(PermissionProcessStatus.UNFULFILLABLE);
    }

    @Test
    void apDataNeed_400_marksUnfulfillable() {
        primeFetchFailure(new EtaPlusBadRequestException("400", 400, new RuntimeException()));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        assertNoStreamPublish();
        assertCommitted(PermissionProcessStatus.UNFULFILLABLE);
    }

    @Test
    void apDataNeed_429_marksUnableToSend() {
        primeFetchFailure(new RateLimitException("429"));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        assertNoStreamPublish();
        assertCommitted(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    @Test
    void apDataNeed_5xx_marksUnableToSend() {
        primeFetchFailure(new EtaPlusServerException("500", 500, new RuntimeException()));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        assertCommitted(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    @Test
    void apDataNeed_timeout_marksUnableToSend() {
        primeFetchFailure(new EtaPlusTimeoutException("timeout", new RuntimeException()));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        assertCommitted(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    @Test
    void apDataNeed_deserializationError_marksUnableToSend() {
        primeFetchFailure(new DeserializationException("bad", new RuntimeException()));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        assertCommitted(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    @Test
    void apDataNeed_unexpectedException_marksUnableToSend() {
        primeFetchFailure(new RuntimeException("boom"));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        assertCommitted(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    @Test
    void apDataNeed_401_thenRefreshSucceeds_thenSecondFetchSucceeds_marksFulfilled() {
        when(repository.findByPermissionId(PID)).thenReturn(Optional.of(pr));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(apDataNeed);
        when(apiClient.fetchAccountingPointData(pr, ACCESS_TOKEN))
                .thenReturn(Mono.error(new AuthenticationException("401", 401, new RuntimeException())));
        when(authService.refresh(REFRESH_TOKEN))
                .thenReturn(Mono.just(new AuthTokenResponse(
                        new AuthTokenResponse.TokenData("new-access", "new-refresh"), true)));
        EtaPlusAccountingPointData payload = samplePayload();
        when(apiClient.fetchAccountingPointData(pr, "new-access")).thenReturn(Mono.just(payload));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        verify(stream).publish(pr, payload);
        assertCommitted(PermissionProcessStatus.FULFILLED);
        verify(apiClient, times(1)).fetchAccountingPointData(pr, ACCESS_TOKEN);
        verify(apiClient, times(1)).fetchAccountingPointData(pr, "new-access");
    }

    @Test
    void apDataNeed_401_refreshFails_marksUnableToSend() {
        when(repository.findByPermissionId(PID)).thenReturn(Optional.of(pr));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(apDataNeed);
        when(apiClient.fetchAccountingPointData(pr, ACCESS_TOKEN))
                .thenReturn(Mono.error(new AuthenticationException("401", 401, new RuntimeException())));
        when(authService.refresh(REFRESH_TOKEN))
                .thenReturn(Mono.just(new AuthTokenResponse(null, false)));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        // Initial fetch with the original token, no retry with a different token
        verify(apiClient, times(1)).fetchAccountingPointData(pr, ACCESS_TOKEN);
        verify(apiClient, times(1)).fetchAccountingPointData(eq(pr), anyString());
        assertNoStreamPublish();
        assertCommitted(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    @Test
    void apDataNeed_401_refreshSucceeds_butSecondFetch401_marksUnableToSend() {
        when(repository.findByPermissionId(PID)).thenReturn(Optional.of(pr));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(apDataNeed);
        when(apiClient.fetchAccountingPointData(pr, ACCESS_TOKEN))
                .thenReturn(Mono.error(new AuthenticationException("401", 401, new RuntimeException())));
        when(authService.refresh(REFRESH_TOKEN))
                .thenReturn(Mono.just(new AuthTokenResponse(
                        new AuthTokenResponse.TokenData("new-access", "new-refresh"), true)));
        when(apiClient.fetchAccountingPointData(pr, "new-access"))
                .thenReturn(Mono.error(new AuthenticationException("401-again", 401, new RuntimeException())));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, REFRESH_TOKEN));

        assertNoStreamPublish();
        assertCommitted(PermissionProcessStatus.UNABLE_TO_SEND);
        // Verify no third fetch attempt: only the two we set up
        verify(apiClient, times(1)).fetchAccountingPointData(pr, ACCESS_TOKEN);
        verify(apiClient, times(1)).fetchAccountingPointData(pr, "new-access");
    }

    @Test
    void apDataNeed_401_noRefreshToken_marksUnableToSend() {
        DePermissionRequest prWithoutRefresh = new DePermissionRequestBuilder()
                .permissionId(PID)
                .meteringPointId(MPID)
                .dataNeedId(DATA_NEED_ID)
                .accessToken(ACCESS_TOKEN)
                .build();
        when(repository.findByPermissionId(PID)).thenReturn(Optional.of(prWithoutRefresh));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(apDataNeed);
        when(apiClient.fetchAccountingPointData(prWithoutRefresh, ACCESS_TOKEN))
                .thenReturn(Mono.error(new AuthenticationException("401", 401, new RuntimeException())));

        emit(new AcceptedEvent(PID, ACCESS_TOKEN, null));

        verifyNoInteractions(authService);
        assertNoStreamPublish();
        assertCommitted(PermissionProcessStatus.UNABLE_TO_SEND);
    }

    // ----- helpers -----

    private void primeFetchFailure(Throwable error) {
        when(repository.findByPermissionId(PID)).thenReturn(Optional.of(pr));
        when(dataNeedsService.getById(DATA_NEED_ID)).thenReturn(apDataNeed);
        when(apiClient.fetchAccountingPointData(pr, ACCESS_TOKEN)).thenReturn(Mono.error(error));
    }

    private void assertNoStreamPublish() {
        verify(stream, never()).publish(any(DePermissionRequest.class), any(EtaPlusAccountingPointData.class));
    }

    private void assertCommitted(PermissionProcessStatus expected) {
        ArgumentCaptor<SimpleEvent> captor = ArgumentCaptor.forClass(SimpleEvent.class);
        verify(outbox).commit(captor.capture());
        SimpleEvent committed = captor.getValue();
        assertThat(committed.permissionId()).isEqualTo(PID);
        assertThat(committed.status()).isEqualTo(expected);
    }
}