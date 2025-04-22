package energy.eddie.regionconnector.be.fluvius.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.service.polling.PollingService;
import energy.eddie.regionconnector.shared.retransmission.PollingFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.be.fluvius.FluviusRegionConnectorMetadata.ZONE_ID_BELGIUM;

@Component
public class RetransmissionPollingService implements PollingFunction<FluviusPermissionRequest> {
    private final PollingService pollingService;

    public RetransmissionPollingService(PollingService pollingService) {this.pollingService = pollingService;}

    @Override
    public Mono<RetransmissionResult> poll(
            FluviusPermissionRequest permissionRequest,
            RetransmissionRequest retransmissionRequest
    ) {
        var permissionId = permissionRequest.permissionId();
        return pollingService.forcePoll(permissionRequest, retransmissionRequest.from(), retransmissionRequest.to())
                             .collectList()
                             .filter(responses -> !responses.isEmpty())
                             .map(ignored -> new Success(permissionId,
                                                         ZonedDateTime.now(ZONE_ID_BELGIUM)))
                             .cast(RetransmissionResult.class)
                             .defaultIfEmpty(
                                     new DataNotAvailable(permissionId, ZonedDateTime.now(ZONE_ID_BELGIUM))
                             )
                             .onErrorResume(error -> Mono.just(new Failure(permissionId,
                                                                           ZonedDateTime.now(ZONE_ID_BELGIUM),
                                                                           error.getMessage())));
    }
}
