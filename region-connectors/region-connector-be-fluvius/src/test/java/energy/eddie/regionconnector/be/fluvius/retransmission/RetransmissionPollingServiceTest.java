package energy.eddie.regionconnector.be.fluvius.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.be.fluvius.client.model.ApiMetaData;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.service.polling.PollingService;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetransmissionPollingServiceTest {
    @Mock
    private PollingService pollingService;
    @InjectMocks
    private RetransmissionPollingService retransmissionPollingService;

    @Test
    void testPoll_ForEmptyResponse_returnsDataNotAvailable() {
        // Given
        var pr = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("be-fluvius", "pid", from, to);
        when(pollingService.forcePoll(pr, from, to))
                .thenReturn(Flux.empty());
        // When
        var res = retransmissionPollingService.poll(pr, request);

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
        var pr = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("be-fluvius", "pid", from, to);
        when(pollingService.forcePoll(pr, from, to))
                .thenReturn(Flux.error(new RuntimeException("message")));
        // When
        var res = retransmissionPollingService.poll(pr, request);

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
        var pr = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("be-fluvius", "pid", from, to);
        when(pollingService.forcePoll(pr, from, to))
                .thenReturn(Flux.just(
                        new GetEnergyResponseModelApiDataResponse(
                                new ApiMetaData(null),
                                new GetEnergyResponseModel(null, null, null)
                        )
                ));
        // When
        var res = retransmissionPollingService.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .asInstanceOf(InstanceOfAssertFactories.type(Success.class))
                            .extracting(Success::permissionId)
                            .isEqualTo("pid")
                    )
                    .verifyComplete();
    }
}
