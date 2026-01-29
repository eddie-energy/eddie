// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequestRouter;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionServiceNotFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service routes retransmission messages between the region connectors. It does that by using the
 * regionConnectorId, which should match the region connector id.
 */
@Service
public class CoreRetransmissionRouter implements RetransmissionRequestRouter, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreRetransmissionRouter.class);
    private final Map<String, RegionConnectorRetransmissionService> retransmissionServices = new HashMap<>();
    private final Sinks.Many<RetransmissionResult> retransmissionResultSink = Sinks.many()
                                                                                   .multicast()
                                                                                   .onBackpressureBuffer();

    // Share the flux to avoid multiple subscriptions triggering multiple retransmissions
    private final Flux<RetransmissionResult> sharedRetransmissionResultFlux = retransmissionResultSink.asFlux().share();

    private final List<Disposable> subscriptions = new ArrayList<>();

    public void registerRetransmissionService(
            String regionConnectorId,
            RegionConnectorRetransmissionService regionConnectorRetransmissionService
    ) {
        LOGGER.info("{}: Registering RegionConnectorRetransmissionService: {}",
                    regionConnectorId,
                    regionConnectorRetransmissionService.getClass().getName());
        retransmissionServices.put(regionConnectorId, regionConnectorRetransmissionService);
    }

    public void registerRetransmissionConnector(
            RetransmissionOutboundConnector retransmissionOutboundConnector
    ) {
        LOGGER.info("Registering RetransmissionConnector: {}",
                    retransmissionOutboundConnector.getClass().getName());
        var subscription = retransmissionOutboundConnector.retransmissionRequests()
                                                          .subscribe(this::routeRetransmissionRequest);
        subscriptions.add(subscription);
    }

    @Override
    public Flux<RetransmissionResult> retransmissionResults() {
        return sharedRetransmissionResultFlux;
    }

    @Override
    public void close() throws Exception {
        retransmissionResultSink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
        for (Disposable subscription : subscriptions) {
            subscription.dispose();
        }
    }

    private void routeRetransmissionRequest(RetransmissionRequest retransmissionRequest) {
        String regionConnectorId = retransmissionRequest.regionConnectorId();
        LOGGER.info("Routing RetransmissionRequest for region connector ID {}, request: {}",
                    regionConnectorId,
                    retransmissionRequest);

        RegionConnectorRetransmissionService retransmissionService = retransmissionServices.get(regionConnectorId);
        if (retransmissionService == null) {
            retransmissionResultSink.emitNext(
                    new RetransmissionServiceNotFound(
                            retransmissionRequest.permissionId(),
                            regionConnectorId,
                            ZonedDateTime.now(ZoneOffset.UTC)
                    ),
                    Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(5))
            );
            return;
        }

        retransmissionService.requestRetransmission(retransmissionRequest).subscribe(
                retransmissionResultSink::tryEmitNext
        );
    }
}
