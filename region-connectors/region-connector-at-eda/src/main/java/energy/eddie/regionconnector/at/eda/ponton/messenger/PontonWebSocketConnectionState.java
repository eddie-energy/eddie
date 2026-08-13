// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import jakarta.annotation.Nullable;

import java.time.Duration;
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
    private boolean receptionStarted;
    @Nullable
    private Instant lastStartAt;
    @Nullable
    private Instant lastRestartAt;
    @Nullable
    private Instant lastConnectionStatusChangedAt;
    @Nullable
    private Instant lastAdapterStatusRequestAt;
    private int outboundConnectionCount;
    private int inboundConnectionCount;
    private int archiveConnectionCount;

    /**
     * Marks that {@code startReception()} was called and the adapter now expects connection-status callbacks.
     */
    synchronized void markReceptionStarted(Instant now) {
        receptionStarted = true;
        lastStartAt = now;
    }

    /**
     * Resets the observed connection counts after rebuilding the Messenger connection.
     */
    synchronized void markRestarted(Instant now) {
        lastRestartAt = now;
        lastStartAt = now;
        lastConnectionStatusChangedAt = null;
        outboundConnectionCount = 0;
        inboundConnectionCount = 0;
        archiveConnectionCount = 0;
    }

    synchronized void markAdapterStatusRequested(Instant now) {
        lastAdapterStatusRequestAt = now;
    }

    /**
     * Stores the current active WebSocket connection counts reported by the Adapter API.
     */
    synchronized void updateConnectionCounts(
            int outboundConnectionCount,
            int inboundConnectionCount,
            int archiveConnectionCount,
            Instant now
    ) {
        this.outboundConnectionCount = outboundConnectionCount;
        this.inboundConnectionCount = inboundConnectionCount;
        this.archiveConnectionCount = archiveConnectionCount;
        lastConnectionStatusChangedAt = now;
    }

    synchronized boolean needsReconnect(
            int expectedOutboundConnections,
            int expectedInboundConnections,
            int expectedArchiveConnections,
            Duration connectionStatusTimeout,
            Instant now
    ) {
        if (!receptionStarted) {
            return false;
        }
        if (lastConnectionStatusChangedAt == null) {
            return lastStartAt != null && lastStartAt.plus(connectionStatusTimeout).isBefore(now);
        }
        return outboundConnectionCount < expectedOutboundConnections ||
               inboundConnectionCount < expectedInboundConnections ||
               archiveConnectionCount < expectedArchiveConnections;
    }

    synchronized HealthCheck healthCheck(
            int expectedOutboundConnections,
            int expectedInboundConnections,
            int expectedArchiveConnections,
            Duration connectionStatusTimeout,
            Instant now
    ) {
        var ok = !needsReconnect(
                expectedOutboundConnections,
                expectedInboundConnections,
                expectedArchiveConnections,
                connectionStatusTimeout,
                now
        );
        return new HealthCheck("adapterWebSocketConnection", ok, describe());
    }

    synchronized String describe() {
        return "started=%s, outbound=%d, inbound=%d, archive=%d, lastStartAt=%s, lastRestartAt=%s, lastConnectionStatusChangedAt=%s, lastAdapterStatusRequestAt=%s"
                .formatted(
                        receptionStarted,
                        outboundConnectionCount,
                        inboundConnectionCount,
                        archiveConnectionCount,
                        lastStartAt,
                        lastRestartAt,
                        lastConnectionStatusChangedAt,
                        lastAdapterStatusRequestAt
                );
    }
}
