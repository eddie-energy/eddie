package energy.eddie.regionconnector.dk.energinet.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.TimeSeries;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequestBuilder;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.services.PollingService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnerginetPollingFunctionTest {
    @Mock
    private PollingService pollingService;
    @InjectMocks
    private EnerginetPollingFunction pollingFunc;

    @Test
    void testPoll_forEmptyResponse_returnsDataNotAvailable() {
        // Given
        var pr = new EnerginetPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("dk-energinet", "pid", from, to);
        when(pollingService.pollTimeSeriesData(pr, from, to))
                .thenReturn(Mono.empty());
        // When
        var res = pollingFunc.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .isInstanceOf(DataNotAvailable.class)
                            .extracting(RetransmissionResult::permissionId)
                            .isEqualTo("pid")
                    )
                    .verifyComplete();
    }

    @Test
    void testPoll_forError_returnsFailure() {
        // Given
        var pr = new EnerginetPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        when(pollingService.pollTimeSeriesData(pr, from, to))
                .thenReturn(Mono.error(new RuntimeException("message")));
        // When
        var res = pollingFunc.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .asInstanceOf(InstanceOfAssertFactories.type(Failure.class))
                            .satisfies(failure -> {
                                assertThat(failure.permissionId()).isEqualTo("pid");
                                assertThat(failure.reason()).isEqualTo("message");
                            })
                    )
                    .verifyComplete();
    }

    @Test
    void testPoll_forValidResponse_returnsSuccess() {
        // Given
        var pr = new EnerginetPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        var payload = new IdentifiableApiResponse(pr, new MyEnergyDataMarketDocumentResponse()
                .success(true)
                .myEnergyDataMarketDocument(new MyEnergyDataMarketDocument()
                                                    .addTimeSeriesItem(new TimeSeries())));
        when(pollingService.pollTimeSeriesData(pr, from, to))
                .thenReturn(Mono.just(payload));
        // When
        var res = pollingFunc.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .asInstanceOf(InstanceOfAssertFactories.type(Success.class))
                            .extracting(Success::permissionId)
                            .isEqualTo("pid")
                    )
                    .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("testPoll_forPartiallyEmptyResponse_returnsDataEmpty")
    void testPoll_forPartiallyEmptyResponse_returnsDataEmpty(IdentifiableApiResponse payload) {
        // Given
        var pr = payload.permissionRequest();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        when(pollingService.pollTimeSeriesData(pr, from, to))
                .thenReturn(Mono.just(payload));
        // When
        var res = pollingFunc.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .isInstanceOf(DataNotAvailable.class)
                            .extracting(RetransmissionResult::permissionId)
                            .isEqualTo("pid")
                    )
                    .verifyComplete();
    }

    private static Stream<Arguments> testPoll_forPartiallyEmptyResponse_returnsDataEmpty() {
        var pr = new EnerginetPermissionRequestBuilder().setPermissionId("pid").build();
        var failure = new MyEnergyDataMarketDocumentResponse().success(false);
        var success = new MyEnergyDataMarketDocumentResponse()
                .success(true)
                .myEnergyDataMarketDocument(new MyEnergyDataMarketDocument());
        return Stream.of(
                Arguments.of(new IdentifiableApiResponse(pr, failure)),
                Arguments.of(new IdentifiableApiResponse(pr, success))
        );
    }
}