// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.admin.console.services;

import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class RetransmissionAdminConsoleOutboundConnector implements RetransmissionOutboundConnector, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetransmissionAdminConsoleOutboundConnector.class);

    private final Sinks.Many<RetransmissionRequest> retransmissionRequestsSink = Sinks.many()
                                                                                      .multicast()
                                                                                      .onBackpressureBuffer();
    @Nullable
    private Disposable subscription = null;

    @Override
    public Flux<RetransmissionRequest> retransmissionRequests() {
        return retransmissionRequestsSink.asFlux();
    }

    @Override
    public void setRetransmissionResultStream(Flux<RetransmissionResult> retransmissionResultStream) {
        subscription = retransmissionResultStream
                .doOnError(e -> LOGGER.error("Error while requesting retransmission.", e))
                .onErrorResume(e -> Flux.empty())
                .subscribe(result ->
                                   // Update this with GH-1388
                                   LOGGER.atInfo()
                                         .addArgument(result.permissionId())
                                         .addArgument(result)
                                         .log("Retransmission request for permissionId '{}' completed with result {}")
                );
    }

    @Override
    public void close() {
        retransmissionRequestsSink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
        if (subscription != null) {
            subscription.dispose();
        }
    }
}
