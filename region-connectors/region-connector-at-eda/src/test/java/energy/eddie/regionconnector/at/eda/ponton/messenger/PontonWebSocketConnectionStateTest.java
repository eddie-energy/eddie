// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PontonWebSocketConnectionStateTest {
    private static final Duration STATUS_TIMEOUT = Duration.ofMinutes(2);
    private static final Instant START = Instant.parse("2026-08-02T10:00:00Z");

    @Test
    void needsReconnect_returnsFalseBeforeReceptionStarted() {
        var state = new PontonWebSocketConnectionState();

        assertThat(state.needsReconnect(1, 1, 0, STATUS_TIMEOUT, START.plus(STATUS_TIMEOUT).plusSeconds(1)))
                .isFalse();
    }

    @Test
    void needsReconnect_returnsTrueWhenInitialConnectionStatusCallbackIsStale() {
        var state = new PontonWebSocketConnectionState();
        state.markReceptionStarted(START);

        assertThat(state.needsReconnect(1, 1, 0, STATUS_TIMEOUT, START.plus(STATUS_TIMEOUT).plusSeconds(1)))
                .isTrue();
    }

    @Test
    void needsReconnect_returnsTrueWhenInboundConnectionCountIsBelowExpected() {
        var state = new PontonWebSocketConnectionState();
        state.markReceptionStarted(START);
        state.updateConnectionCounts(1, 0, 0, START.plusSeconds(1));

        assertThat(state.needsReconnect(1, 1, 0, STATUS_TIMEOUT, START.plusSeconds(2)))
                .isTrue();
    }

    @Test
    void needsReconnect_returnsFalseWhenConnectionCountsMatchExpectedCounts() {
        var state = new PontonWebSocketConnectionState();
        state.markReceptionStarted(START);
        state.updateConnectionCounts(1, 1, 0, START.plusSeconds(1));

        assertThat(state.needsReconnect(1, 1, 0, STATUS_TIMEOUT, START.plusSeconds(2)))
                .isFalse();
    }
}
