// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.services.PollingService;
import energy.eddie.regionconnector.shared.retransmission.PollingFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

@Component
public class EnerginetPollingFunction implements PollingFunction<DkEnerginetPermissionRequest> {
    private final PollingService pollingService;

    public EnerginetPollingFunction(PollingService pollingService) {this.pollingService = pollingService;}

    @Override
    public Mono<RetransmissionResult> poll(
            DkEnerginetPermissionRequest permissionRequest,
            RetransmissionRequest retransmissionRequest
    ) {
        var permissionId = permissionRequest.permissionId();
        return pollingService.pollTimeSeriesData(permissionRequest,
                                                 retransmissionRequest.from(),
                                                 retransmissionRequest.to())
                             .filter(EnerginetPollingFunction::isPresent)
                             .map(ignored -> new Success(permissionId, ZonedDateTime.now(ZoneOffset.UTC)))
                             .cast(RetransmissionResult.class)
                             .onErrorResume(e -> Mono.just(new Failure(permissionId,
                                                                       ZonedDateTime.now(ZoneOffset.UTC),
                                                                       e.getMessage())))
                             .defaultIfEmpty(new DataNotAvailable(permissionId, ZonedDateTime.now(ZoneOffset.UTC)));
    }

    private static boolean isPresent(IdentifiableApiResponse response) {
        var doc = Optional.ofNullable(response.apiResponse().getMyEnergyDataMarketDocument())
                          .flatMap(r -> Optional.ofNullable(r.getTimeSeries()))
                          .map(r -> !r.isEmpty());
        return Boolean.TRUE.equals(response.apiResponse().getSuccess()) && doc.orElse(false);
    }
}
