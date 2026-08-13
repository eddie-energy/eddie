// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PontonWebSocketConnectionStateTest {
    private static final Duration STATUS_TIMEOUT = Duration.ofMinutes(2);
    private static final Instant START = Instant.parse("2026-08-02T10:00:00Z");
    private static final PontonXPAdapterConfiguration CONFIG = new PontonXPAdapterConfiguration(
            "adapter-id", "adapter-version", "localhost", 8080, "api-endpoint", "work-folder", "username", "password",
            1, 1, Duration.ofMinutes(1), STATUS_TIMEOUT
    );

    @Test
    void needsReconnect_returnsFalseBeforeReceptionStarted() {
        var state = new PontonWebSocketConnectionState();

        assertThat(state.needsReconnect(CONFIG, START.plus(STATUS_TIMEOUT).plusSeconds(1)))
                .isFalse();
    }

    @Test
    void needsReconnect_returnsTrueWhenInitialConnectionStatusCallbackIsStale() {
        var state = new PontonWebSocketConnectionState();
        state.markReceptionStarted(START);

        assertThat(state.needsReconnect(CONFIG, START.plus(STATUS_TIMEOUT).plusSeconds(1)))
                .isTrue();
    }

    @Test
    void needsReconnect_returnsTrueWhenInboundConnectionCountIsBelowExpected() {
        var state = new PontonWebSocketConnectionState();
        state.markReceptionStarted(START);
        state.updateConnectionCounts(1, 0, START.plusSeconds(1));

        assertThat(state.needsReconnect(CONFIG, START.plusSeconds(2)))
                .isTrue();
    }

    @Test
    void needsReconnect_returnsFalseWhenConnectionCountsMatchExpectedCounts() {
        var state = new PontonWebSocketConnectionState();
        state.markReceptionStarted(START);
        state.updateConnectionCounts(1, 1, START.plusSeconds(1));

        assertThat(state.needsReconnect(CONFIG, START.plusSeconds(2)))
                .isFalse();
    }
}
