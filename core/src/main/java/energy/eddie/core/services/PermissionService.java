// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Service
public class PermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink = Sinks.many()
                                                                                         .replay()
                                                                                         .limit(Duration.ofSeconds(60));

    public void registerProvider(ConnectionStatusMessageProvider statusMessageProvider) {
        LOGGER.info("PermissionService: Registering {}", statusMessageProvider.getClass().getName());
        statusMessageProvider.getConnectionStatusMessageStream()
                             .onErrorContinue((err, obj) -> LOGGER.warn(
                                     "Encountered error while processing connection status message",
                                     err))
                             .subscribe(connectionStatusMessageSink::tryEmitNext);
    }

    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusMessageSink.asFlux();
    }
}
