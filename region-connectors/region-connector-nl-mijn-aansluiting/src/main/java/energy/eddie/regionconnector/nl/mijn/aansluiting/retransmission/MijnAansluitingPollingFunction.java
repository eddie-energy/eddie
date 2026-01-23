// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import energy.eddie.regionconnector.shared.retransmission.PollingFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class MijnAansluitingPollingFunction implements PollingFunction<MijnAansluitingPermissionRequest> {
    private final PollingService pollingService;

    public MijnAansluitingPollingFunction(PollingService pollingService) {this.pollingService = pollingService;}

    @Override
    public Mono<RetransmissionResult> poll(
            MijnAansluitingPermissionRequest permissionRequest,
            RetransmissionRequest retransmissionRequest
    ) {
        var permissionId = permissionRequest.permissionId();
        return pollingService.pollTimeSeriesData(permissionRequest,
                                                 retransmissionRequest.from(),
                                                 retransmissionRequest.to())
                             .map(ignored -> new Success(permissionId, ZonedDateTime.now(ZoneOffset.UTC)))
                             .cast(RetransmissionResult.class)
                             .onErrorResume(e -> Mono.just(new Failure(permissionId,
                                                                       ZonedDateTime.now(ZoneOffset.UTC),
                                                                       e.getMessage())))
                             .defaultIfEmpty(new DataNotAvailable(permissionId, ZonedDateTime.now(ZoneOffset.UTC)));
    }

}
