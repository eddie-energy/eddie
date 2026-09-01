// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import jakarta.annotation.Nullable;

import java.time.Clock;
import java.time.Instant;

/**
 * Tracks the local adapter-side WebSocket connection state reported by the PONTON X/P Adapter API.
 * <p>
 * The PONTON REST health endpoint only says whether the Messenger process is reachable. It does not prove that this
 * adapter still has usable WebSocket connections for inbound and outbound processing. The PONTON X/P Adapter API exposes
 * {@code onConnectionStatusChanged}, which reports the active WebSocket connection counts for each processing direction.
 * This class stores those callback values and turns them into a reconnect decision.
 * <p>
 * A connection is considered bad when reception has been started and either no initial connection status was reported
 * within the configured timeout, or one of the observed connection counts is below the configured expected count.
 */
final class PontonWebSocketConnectionState {
    private final PontonXPAdapterConfiguration config;
    private final Clock clock;
    @Nullable
    private Instant lastReceptionStartAt;
    @Nullable
    private Instant lastRestartAt;
    @Nullable
    private Instant lastConnectionStatusChangedAt;
    @Nullable
    private Instant lastAdapterStatusRequestAt;
    private int outboundConnectionCount;
    private int inboundConnectionCount;

    PontonWebSocketConnectionState(PontonXPAdapterConfiguration config, Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    /**
     * Marks that {@code startReception()} was called and the adapter now expects connection-status callbacks.
     */
    synchronized void markReceptionStarted() {
        lastReceptionStartAt = clock.instant();
    }

    /**
     * Resets the observed connection counts after rebuilding the Messenger connection.
     */
    synchronized void markRestarted() {
        var now = clock.instant();
        lastRestartAt = now;
        lastReceptionStartAt = now;
        lastConnectionStatusChangedAt = null;
        outboundConnectionCount = 0;
        inboundConnectionCount = 0;
    }

    synchronized void markAdapterStatusRequested() {
        lastAdapterStatusRequestAt = clock.instant();
    }

    /**
     * Stores the current active WebSocket connection counts reported by the Adapter API.
     */
    synchronized void updateConnectionCounts(
            int outboundConnectionCount,
            int inboundConnectionCount
    ) {
        this.outboundConnectionCount = outboundConnectionCount;
        this.inboundConnectionCount = inboundConnectionCount;
        lastConnectionStatusChangedAt = clock.instant();
    }

    synchronized boolean needsReconnect() {
        if (lastReceptionStartAt == null) {
            return false;
        }
        if (lastConnectionStatusChangedAt == null) {
            return lastReceptionStartAt.plus(config.connectionStatusTimeout()).isBefore(clock.instant());
        }
        return outboundConnectionCount < config.outboundConnections() ||
               inboundConnectionCount < config.inboundConnections();
    }

    synchronized HealthCheck healthCheck() {
        var ok = !needsReconnect();
        return new HealthCheck("adapterWebSocketConnection", ok, describe());
    }

    synchronized String describe() {
        return "started=%s, outbound=%d, inbound=%d, lastReceptionStartAt=%s, lastRestartAt=%s, lastConnectionStatusChangedAt=%s, lastAdapterStatusRequestAt=%s"
                .formatted(
                        lastReceptionStartAt != null,
                        outboundConnectionCount,
                        inboundConnectionCount,
                        lastReceptionStartAt,
                        lastRestartAt,
                        lastConnectionStatusChangedAt,
                        lastAdapterStatusRequestAt
                );
    }
}
