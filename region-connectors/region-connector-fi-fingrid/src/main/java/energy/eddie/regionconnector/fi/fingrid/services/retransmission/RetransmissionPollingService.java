package energy.eddie.regionconnector.fi.fingrid.services.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.services.PollingService;
import energy.eddie.regionconnector.shared.retransmission.PollingFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

@Component
public class RetransmissionPollingService implements PollingFunction<FingridPermissionRequest> {
    private final PollingService pollingService;

    public RetransmissionPollingService(PollingService pollingService) {this.pollingService = pollingService;}

    @Override
    public Mono<RetransmissionResult> poll(
            FingridPermissionRequest permissionRequest,
            RetransmissionRequest retransmissionRequest
    ) {
        var permissionId = permissionRequest.permissionId();
        return pollingService.forcePoll(permissionRequest,
                                        retransmissionRequest.from().atStartOfDay(ZoneOffset.UTC),
                                        endOfDay(retransmissionRequest.to(), ZoneOffset.UTC))
                             .filter(responses -> !responses.isEmpty())
                             .map(ignored -> new Success(permissionId, ZonedDateTime.now(ZoneOffset.UTC)))
                             .cast(RetransmissionResult.class)
                             .defaultIfEmpty(
                                     new DataNotAvailable(permissionId, ZonedDateTime.now(ZoneOffset.UTC))
                             )
                             .onErrorResume(error -> Mono.just(new Failure(permissionId,
                                                                           ZonedDateTime.now(ZoneOffset.UTC),
                                                                           error.getMessage())));
    }
}