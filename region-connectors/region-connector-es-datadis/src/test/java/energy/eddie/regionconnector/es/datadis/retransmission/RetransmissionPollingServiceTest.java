package energy.eddie.regionconnector.es.datadis.retransmission;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.PointType;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetransmissionPollingServiceTest {
    @Mock
    private DataApi dataApi;
    @Spy
    private EnergyDataStreams streams = new EnergyDataStreams();
    @InjectMocks
    private RetransmissionPollingService pollingService;

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"FULFILLED", "ACCEPTED"})
    void requestRetransmission_fetchesData_emitsExpectedDataAndReturnsSuccess(PermissionProcessStatus status) throws IOException {
        // Given
        var meteringData = MeteringDataProvider.loadMeteringData();
        var meteringDataStart = meteringData.getFirst().date();
        var meteringDataEnd = meteringData.getLast().date();
        var retransmitFrom = meteringDataStart.plusDays(2);
        var retransmitTo = retransmitFrom.plusDays(2);

        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(meteringDataStart,
                                                  meteringDataEnd.plusWeeks(1),
                                                  status);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.just(meteringData));

        var request = new RetransmissionRequest("rc-id", "pid", retransmitFrom, retransmitTo);

        // When
        var res = pollingService.poll(permissionRequest, request);

        // Then
        var emitVerifier = StepVerifier.create(streams.getValidatedHistoricalData())
                                       .assertNext(result -> assertAll(
                                               () -> assertEquals(retransmitFrom, result.payload().start()),
                                               () -> assertEquals(retransmitTo, result.payload().end()),
                                               () -> assertEquals("pid", result.permissionRequest().permissionId())
                                       ))
                                       .then(() -> streams.close());
        StepVerifier
                .create(res)
                .assertNext(result -> assertThat(result)
                        .asInstanceOf(InstanceOfAssertFactories.type(Success.class))
                        .satisfies(success -> {
                            assertThat(success.permissionId()).isEqualTo("pid");
                            assertThat(success.timestamp()).isBetween(before, ZonedDateTime.now(ZONE_ID_SPAIN));
                        })
                )
                .verifyComplete();

        emitVerifier.verifyComplete();
    }

    @Test
    void requestRetransmission_fetchesDataEmitFails_returnsFailure() throws IOException {
        // Given
        var meteringData = MeteringDataProvider.loadMeteringData();
        var meteringDataStart = meteringData.getFirst().date();
        var meteringDataEnd = meteringData.getLast().date();
        var retransmitFrom = meteringDataStart.plusDays(2);
        var retransmitTo = retransmitFrom.plusDays(2);

        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(meteringDataStart,
                                                  meteringDataEnd.plusWeeks(1),
                                                  PermissionProcessStatus.ACCEPTED);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.just(meteringData));
        var request = new RetransmissionRequest("rc-id", "pid", retransmitFrom, retransmitTo);
        streams.close();

        // When
        var res = pollingService.poll(permissionRequest, request);

        // Then
        StepVerifier
                .create(res)
                .assertNext(result -> assertThat(result)
                        .asInstanceOf(InstanceOfAssertFactories.type(Failure.class))
                        .satisfies(failure -> {
                            assertThat(failure.permissionId()).isEqualTo("pid");
                            assertThat(failure.reason()).isEqualTo("Could not emit fetched data");
                            assertThat(failure.timestamp()).isBetween(before, ZonedDateTime.now(ZONE_ID_SPAIN));
                        })
                )
                .verifyComplete();
    }


    @Test
    void requestRetransmission_fetchesEmptyData_emitsDataNotAvailable() {
        // Given
        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var today = LocalDate.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(today.minusWeeks(1),
                                                  today.plusWeeks(1),
                                                  PermissionProcessStatus.ACCEPTED);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.just(List.of()));

        var request = new RetransmissionRequest("rc-id", "pid", today.minusDays(1), today.minusDays(1));
        // When
        var res = pollingService.poll(permissionRequest, request);

        // Then
        StepVerifier
                .create(res)
                .assertNext(result -> assertThat(result)
                        .asInstanceOf(InstanceOfAssertFactories.type(DataNotAvailable.class))
                        .satisfies(dataNotAvailable -> {
                            assertThat(dataNotAvailable.permissionId()).isEqualTo("pid");
                            assertThat(dataNotAvailable.timestamp()).isBetween(before,
                                                                               ZonedDateTime.now(ZONE_ID_SPAIN));
                        })
                )
                .verifyComplete();
    }

    @Test
    void requestRetransmission_fetchesDataButRequestedDataNotReturned_emitsDataNotAvailable() throws IOException {
        // Given
        var meteringData = MeteringDataProvider.loadMeteringData();
        var meteringDataStart = meteringData.getFirst().date();
        var meteringDataEnd = meteringData.getLast().date();

        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(meteringDataStart,
                                                  meteringDataEnd.plusWeeks(2),
                                                  PermissionProcessStatus.ACCEPTED);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.just(meteringData));

        var request = new RetransmissionRequest("rc-id",
                                                "pid",
                                                meteringDataEnd.plusDays(1),
                                                meteringDataEnd.plusWeeks(1));

        // When
        var res = pollingService.poll(permissionRequest, request);

        // Then
        StepVerifier
                .create(res)
                .assertNext(result -> assertThat(result)
                        .asInstanceOf(InstanceOfAssertFactories.type(DataNotAvailable.class))
                        .satisfies(dataNotAvailable -> {
                            assertThat(dataNotAvailable.permissionId()).isEqualTo("pid");
                            assertThat(dataNotAvailable.timestamp()).isBetween(before,
                                                                               ZonedDateTime.now(ZONE_ID_SPAIN));
                        })
                )
                .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("apiErrors")
    void requestRetransmission_fetchErrors_emitsFailure(Throwable throwable, String expectedReason) {
        var before = ZonedDateTime.now(ZONE_ID_SPAIN);
        var today = LocalDate.now(ZONE_ID_SPAIN);
        var permissionRequest = permissionRequest(today.minusWeeks(1),
                                                  today.plusWeeks(1),
                                                  PermissionProcessStatus.ACCEPTED);
        when(dataApi.getConsumptionKwh(any())).thenReturn(Mono.error(throwable));
        var request = new RetransmissionRequest("rc-id", "pid", today.minusDays(1), today.minusDays(1));
        // When
        var res = pollingService.poll(permissionRequest, request);


        // Then
        StepVerifier
                .create(res)
                .assertNext(result -> assertThat(result)
                        .asInstanceOf(InstanceOfAssertFactories.type(Failure.class))
                        .satisfies(failure -> {
                            assertThat(failure.permissionId()).isEqualTo("pid");
                            assertThat(failure.reason()).isEqualTo(expectedReason);
                            assertThat(failure.timestamp()).isBetween(before, ZonedDateTime.now(ZONE_ID_SPAIN));
                        })
                )
                .verifyComplete();
    }

    private static Stream<Arguments> apiErrors() {
        var forbidden = new DatadisApiException("FORBIDDEN", HttpResponseStatus.FORBIDDEN, "");
        return Stream.of(
                Arguments.of(new Throwable("reason"), "reason"),
                Arguments.of(new DatadisApiException("", HttpResponseStatus.TOO_MANY_REQUESTS, ""),
                             "Datadis returned: '429 Too Many Requests'. Try again in 24 hours."),
                Arguments.of(forbidden, forbidden.getMessage())
        );
    }

    private static EsPermissionRequest permissionRequest(
            LocalDate start,
            LocalDate end,
            PermissionProcessStatus status
    ) {
        return new DatadisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setStart(start)
                .setEnd(end)
                .setStatus(status)
                .setDistributorCode(DistributorCode.ASEME)
                .setGranularity(Granularity.PT15M)
                .setPointType(PointType.TYPE_1)
                .build();
    }
}