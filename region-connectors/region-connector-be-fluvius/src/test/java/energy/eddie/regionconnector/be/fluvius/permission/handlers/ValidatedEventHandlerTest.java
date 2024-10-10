package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.DataNeedNotSupportedResult;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.be.fluvius.client.model.FluviusSessionCreateResultResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.FluviusSessionCreateResultResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.permission.events.InvalidEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidatedEventHandlerTest {
    private final Timeframe permissionTimeFrame = new Timeframe(LocalDate.now(ZoneOffset.UTC),
                                                                LocalDate.now(ZoneOffset.UTC));
    private final Timeframe energyTimeFrame = new Timeframe(LocalDate.now(ZoneOffset.UTC),
                                                            LocalDate.now(ZoneOffset.UTC));
    @Mock
    private FluviusApi fluviusApi;
    @Mock
    private BePermissionRequestRepository bePermissionRequestRepository;
    @Mock
    private DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @Mock
    private Outbox outbox;
    @SuppressWarnings("unused")
    @InjectMocks
    private ValidatedEventHandler validatedEventHandler;
    @Captor
    private ArgumentCaptor<SimpleEvent> simpleCaptor;

    public static Stream<Arguments> testAccept_validRequest_isSentToPa() {
        var now = LocalDate.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(now.minusDays(1)),
                Arguments.of(now.plusDays(1))
        );
    }

    public static Stream<Arguments> testAccept_unexpectedRequestException_unableToSend() {
        return Stream.of(
                Arguments.of(WebClientResponseException.create(500, "500 Error", null, null, null)),
                Arguments.of(new RuntimeException())
        );
    }

    @Test
    void testAccept_UnsupportedDataNeed_doesNothing() {
        // Given
        when(bePermissionRequestRepository.getByPermissionId("pid")).thenReturn(createPermissionRequest());
        when(dataNeedCalculationService.calculate("did")).thenReturn(new DataNeedNotSupportedResult("Not Found!"));

        // When
        eventBus.emit(createValidatedEvent());

        // Then
        verify(outbox, never()).commit(any());
    }

    @ParameterizedTest
    @MethodSource
    void testAccept_validRequest_isSentToPa(LocalDate start) {
        // Given
        var customEnergyTimeframe = new Timeframe(start, LocalDate.now(ZoneOffset.UTC));

        when(bePermissionRequestRepository.getByPermissionId("pid")).thenReturn(createPermissionRequest());
        when(dataNeedCalculationService.calculate("did")).thenReturn(new
                                                                             ValidatedHistoricalDataDataNeedResult(List.of(
                Granularity.PT15M), permissionTimeFrame, customEnergyTimeframe)
        );
        when(fluviusApi.shortUrlIdentifier(eq("pid"), eq(Flow.B2C), any(), any())).thenReturn(
                Mono.just(createPermissionResponse())
        );

        // When
        eventBus.emit(createValidatedEvent());

        // Then
        verify(outbox).commit(isA(SentToPaEvent.class));
    }

    @Test
    void testAccept_invalidRequest_sentToPa_and_invalidEvent() {
        // Given
        when(bePermissionRequestRepository.getByPermissionId("pid")).thenReturn(createPermissionRequest());
        when(dataNeedCalculationService.calculate("did")).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                          permissionTimeFrame,
                                                          energyTimeFrame)
        );
        when(fluviusApi.shortUrlIdentifier(eq("pid"), eq(Flow.B2C), any(), any())).thenReturn(
                Mono.error(WebClientResponseException.create(400, "400 Error", null, null, null))
        );

        // When
        eventBus.emit(createValidatedEvent());

        // Then
        verify(outbox).commit(simpleCaptor.capture());
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, simpleCaptor.getValue().status());

        verify(outbox).commit(isA(InvalidEvent.class));
    }

    @Test
    void testAccept_invalidResponse_sentToPa_and_invalidEvent() {
        // Given
        when(bePermissionRequestRepository.getByPermissionId("pid")).thenReturn(createPermissionRequest());
        when(dataNeedCalculationService.calculate("did")).thenReturn(new
                                                                             ValidatedHistoricalDataDataNeedResult(List.of(
                Granularity.PT15M), permissionTimeFrame, energyTimeFrame)
        );
        when(fluviusApi.shortUrlIdentifier(eq("pid"), eq(Flow.B2C), any(), any())).thenReturn(
                // Result without data means that the response is invalid
                Mono.just(new FluviusSessionCreateResultResponseModelApiDataResponse())
        );

        // When
        eventBus.emit(createValidatedEvent());

        // Then
        verify(outbox).commit(simpleCaptor.capture());
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, simpleCaptor.getValue().status());

        verify(outbox).commit(isA(InvalidEvent.class));
    }

    @ParameterizedTest
    @MethodSource
    void testAccept_unexpectedRequestException_unableToSend(Exception ex) {
        // Given
        when(bePermissionRequestRepository.getByPermissionId("pid")).thenReturn(createPermissionRequest());
        when(dataNeedCalculationService.calculate("did")).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                          permissionTimeFrame,
                                                          energyTimeFrame)
        );
        when(fluviusApi.shortUrlIdentifier(eq("pid"), eq(Flow.B2C), any(), any())).thenReturn(
                Mono.error(ex)
        );

        // When
        eventBus.emit(createValidatedEvent());

        // Then
        verify(outbox).commit(assertArg(event ->
                                                assertEquals(PermissionProcessStatus.UNABLE_TO_SEND, event.status()))
        );
    }

    private FluviusPermissionRequest createPermissionRequest() {
        return new FluviusPermissionRequest(
                "pid",
                "cid",
                "did",
                PermissionProcessStatus.VALIDATED,
                Granularity.PT15M,
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                ZonedDateTime.now(ZoneOffset.UTC),
                Flow.B2C
        );
    }

    private FluviusSessionCreateResultResponseModelApiDataResponse createPermissionResponse() {
        var responseModel = new FluviusSessionCreateResultResponseModel().shortUrlIdentifier("shortUrlIdentifier");
        return new FluviusSessionCreateResultResponseModelApiDataResponse().data(responseModel);
    }

    private ValidatedEvent createValidatedEvent() {
        return new ValidatedEvent("pid",
                                  LocalDate.now(ZoneOffset.UTC),
                                  LocalDate.now(ZoneOffset.UTC),
                                  Granularity.PT15M,
                                  Flow.B2C
        );
    }
}