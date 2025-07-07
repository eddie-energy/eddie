package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.fi.fingrid.client.model.*;
import energy.eddie.regionconnector.fi.fingrid.permission.events.UpdateGranularityEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateGranularityServiceTest {
    public static final Party SENDER = new Party("sender");
    public static final Party RECEIVER = new Party("receiver");
    public static final Header HEADER = new Header("bla",
                                                   SENDER,
                                                   SENDER,
                                                   RECEIVER,
                                                   RECEIVER,
                                                   "bla",
                                                   ZonedDateTime.now(ZoneOffset.UTC),
                                                   "bla",
                                                   "orgUser");
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @InjectMocks
    private UpdateGranularityService updateGranularityService;
    @Captor
    private ArgumentCaptor<UpdateGranularityEvent> eventCaptor;

    public static Stream<Arguments> updateGranularityReturnsResponseAsIs_onValidResponse() {
        return Stream.of(
                Arguments.of(
                        new TimeSeriesResponse(
                                new TimeSeriesData(
                                        HEADER,
                                        new TimeSeriesTransaction("unknown-reason", null, null)
                                )
                        )
                ),
                Arguments.of(
                        new TimeSeriesResponse(
                                new TimeSeriesData(
                                        HEADER,
                                        new TimeSeriesTransaction(null, null, List.of())
                                )
                        )
                )
        );
    }

    public static Stream<Arguments> updateGranularity_emitsUnfulfillable_onEmptyResponse() {
        return Stream.of(
                Arguments.of(
                        new DataNeedNotSupportedResult("Not supported"),
                        new DataNeedNotFoundResult()
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void updateGranularityReturnsResponseAsIs_onValidResponse(TimeSeriesResponse response) {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(now)
                                                      .setEnd(now)
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT15M)
                                                      .setLastMeterReadings(null)
                                                      .createFingridPermissionRequest();
        // When
        var res = updateGranularityService.updateGranularity(List.of(response), pr);

        // Then
        StepVerifier.create(res)
                    .assertNext(resp -> assertEquals(List.of(response), resp))
                    .verifyComplete();
    }

    @Test
    void updateGranularity_emitsNewGranularity_onEmptyResponse() {
        // Given
        var response = new TimeSeriesResponse(
                new TimeSeriesData(
                        HEADER,
                        new TimeSeriesTransaction(EventReason.EMPTY_RESPONSE_REASON, null, null)
                )
        );
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(LocalDate.now(ZoneOffset.UTC))
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT15M)
                                                      .setLastMeterReadings(null)
                                                      .createFingridPermissionRequest();

        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT5M, Granularity.PT15M, Granularity.PT1H),
                null,
                null
        );
        when(calculationService.calculate("dnid"))
                .thenReturn(calc);

        // When
        var res = updateGranularityService.updateGranularity(List.of(response), pr);

        // Then
        StepVerifier.create(res)
                    .verifyComplete();
        verify(outbox).commit(eventCaptor.capture());
        assertEquals(Granularity.PT1H, eventCaptor.getValue().granularity());
    }

    @ParameterizedTest
    @MethodSource
    void updateGranularity_emitsUnfulfillable_onEmptyResponse(DataNeedCalculationResult calc) {
        // Given
        var response = new TimeSeriesResponse(
                new TimeSeriesData(
                        HEADER,
                        new TimeSeriesTransaction(EventReason.EMPTY_RESPONSE_REASON, null, null)
                )
        );
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(LocalDate.now(ZoneOffset.UTC))
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT1H)
                                                      .setLastMeterReadings(null)
                                                      .createFingridPermissionRequest();
        when(calculationService.calculate("dnid"))
                .thenReturn(calc);

        // When
        var res = updateGranularityService.updateGranularity(List.of(response), pr);

        // Then
        StepVerifier.create(res)
                    .verifyComplete();
        verify(outbox)
                .commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())));
    }

    @Test
    void updateGranularity_withNoHigherGranularityAvailable_emitsUnfulfillable() {
        // Given
        var response = new TimeSeriesResponse(
                new TimeSeriesData(
                        HEADER,
                        new TimeSeriesTransaction(EventReason.EMPTY_RESPONSE_REASON, null, null)
                )
        );
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                      .setConnectionId("cid")
                                                      .setDataNeedId("dnid")
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .setStart(LocalDate.now(ZoneOffset.UTC))
                                                      .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                      .setCustomerIdentification("cid")
                                                      .setGranularity(Granularity.PT15M)
                                                      .setLastMeterReadings(null)
                                                      .createFingridPermissionRequest();

        var calc = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT5M, Granularity.PT15M),
                null,
                null
        );
        when(calculationService.calculate("dnid")).thenReturn(calc);

        // When
        var res = updateGranularityService.updateGranularity(List.of(response), pr);

        // Then
        StepVerifier.create(res)
                    .verifyComplete();
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNFULFILLABLE, event.status())));
    }
}