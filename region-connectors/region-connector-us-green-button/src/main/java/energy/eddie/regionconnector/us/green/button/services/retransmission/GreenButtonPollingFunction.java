// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.shared.retransmission.PollingFunction;
import energy.eddie.regionconnector.us.green.button.atom.feed.Query;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.services.PollingService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

@Component
public class GreenButtonPollingFunction implements PollingFunction<UsGreenButtonPermissionRequest> {
    private final PollingService pollingService;

    public GreenButtonPollingFunction(PollingService pollingService) {this.pollingService = pollingService;}

    @Override
    public Mono<RetransmissionResult> poll(
            UsGreenButtonPermissionRequest permissionRequest,
            RetransmissionRequest retransmissionRequest
    ) {
        var permissionId = permissionRequest.permissionId();
        return pollingService.forcePollValidatedHistoricalData(permissionRequest,
                                                               retransmissionRequest.from()
                                                                                    .atStartOfDay(ZoneOffset.UTC),
                                                               endOfDay(retransmissionRequest.to(), ZoneOffset.UTC))
                             .collectList()
                             .filter(this::isPresent)
                             .map(ignored -> new Success(permissionId, ZonedDateTime.now(ZoneOffset.UTC)))
                             .cast(RetransmissionResult.class)
                             .onErrorResume(e -> Mono.just(new Failure(permissionId,
                                                                       ZonedDateTime.now(ZoneOffset.UTC),
                                                                       e.getMessage())))
                             .defaultIfEmpty(new DataNotAvailable(permissionId, ZonedDateTime.now(ZoneOffset.UTC)));
    }

    private boolean isPresent(List<IdentifiableSyndFeed> identifiableSyndFeeds) {
        return identifiableSyndFeeds.stream()
                                    .map(feed -> Query.countEntriesOfType(feed.payload(), "IntervalBlock"))
                                    .anyMatch(count -> count > 0);
    }
}
