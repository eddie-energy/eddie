// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.outbound;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import reactor.core.publisher.Flux;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors are passed on to
 * the EP using their transport of choice, e.g. Kafka.
 */
public interface ConnectionStatusMessageOutboundConnector {
    /**
     * Sets the stream of connection status messages to be sent to the EP app.
     *
     * @param connectionStatusMessageStream stream of connection status messages
     */
    void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> connectionStatusMessageStream);
}
