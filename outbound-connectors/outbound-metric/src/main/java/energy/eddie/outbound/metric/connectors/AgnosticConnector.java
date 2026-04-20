// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.metric.connectors;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AgnosticConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgnosticConnector.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink = Sinks.many()
            .multicast().onBackpressureBuffer();

    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusMessageSink.asFlux();
    }

    @MessageStream(ConnectionStatusMessage.class)
    public void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> connectionStatusMessageStream) {
        connectionStatusMessageStream
                .onErrorContinue((err, obj) -> LOGGER.warn("Got error while processing connection status", err))
                .subscribe(connectionStatusMessageSink::tryEmitNext);
    }
}
