package energy.eddie.regionconnector.cds.services.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.services.PollingService;
import energy.eddie.regionconnector.shared.retransmission.PollingFunction;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple5;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

@Service
public class RetransmissionPollingService implements PollingFunction<CdsPermissionRequest> {
    private final PollingService pollingService;

    public RetransmissionPollingService(PollingService pollingService) {this.pollingService = pollingService;}

    @Override
    public Mono<RetransmissionResult> poll(
            CdsPermissionRequest permissionRequest,
            RetransmissionRequest retransmissionRequest
    ) {
        var permissionId = permissionRequest.permissionId();
        return pollingService.pollTimeSeriesData(permissionRequest,
                                                 retransmissionRequest.from().atStartOfDay(ZoneOffset.UTC),
                                                 endOfDay(retransmissionRequest.to(), ZoneOffset.UTC))
                             .filter(RetransmissionPollingService::allArePresent)
                             .map(ignored -> new Success(permissionId, ZonedDateTime.now(ZoneOffset.UTC)))
                             .cast(RetransmissionResult.class)
                             .defaultIfEmpty(new DataNotAvailable(permissionId, ZonedDateTime.now(ZoneOffset.UTC)))
                             .onErrorResume(error -> Mono.just(new Failure(permissionId,
                                                                           ZonedDateTime.now(ZoneOffset.UTC),
                                                                           error.getMessage())));
    }

    private static boolean allArePresent(Tuple5<List<AccountsEndpoint200ResponseAllOfAccountsInner>, List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner>, List<ServicePointEndpoint200ResponseAllOfServicePointsInner>, List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner>, List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner>> res) {
        return !(
                res.getT1().isEmpty()
                || res.getT2().isEmpty()
                || res.getT3().isEmpty()
                || res.getT4().isEmpty()
                || res.getT5().isEmpty()
        );
    }
}
