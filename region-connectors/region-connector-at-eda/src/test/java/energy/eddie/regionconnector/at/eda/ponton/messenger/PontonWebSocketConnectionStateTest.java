// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;

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
        var clock = new MutableClock(START.plus(STATUS_TIMEOUT).plusSeconds(1));
        var state = new PontonWebSocketConnectionState(CONFIG, clock);

        assertThat(state.needsReconnect()).isFalse();
        assertThat(state.describe()).contains(
                "started=false",
                "lastReceptionStartAt=null"
        );
    }

    @Test
    void needsReconnect_returnsTrueWhenInitialConnectionStatusCallbackIsStale() {
        var clock = new MutableClock(START);
        var state = new PontonWebSocketConnectionState(CONFIG, clock);
        state.markReceptionStarted();
        clock.set(START.plus(STATUS_TIMEOUT).plusSeconds(1));

        assertThat(state.needsReconnect()).isTrue();
    }

    @Test
    void needsReconnect_returnsTrueWhenInboundConnectionCountIsBelowExpected() {
        var clock = new MutableClock(START);
        var state = new PontonWebSocketConnectionState(CONFIG, clock);
        state.markReceptionStarted();
        clock.set(START.plusSeconds(1));
        state.updateConnectionCounts(1, 0);

        assertThat(state.needsReconnect()).isTrue();
    }

    @Test
    void needsReconnect_returnsFalseWhenConnectionCountsMatchExpectedCounts() {
        var clock = new MutableClock(START);
        var state = new PontonWebSocketConnectionState(CONFIG, clock);
        state.markReceptionStarted();
        state.updateConnectionCounts(1, 1);

        assertThat(state.needsReconnect()).isFalse();
    }

    @Test
    void needsReconnect_returnsTrueWhenOutboundConnectionCountIsBelowExpected() {
        var clock = new MutableClock(START);
        var state = new PontonWebSocketConnectionState(CONFIG, clock);
        state.markReceptionStarted();
        state.updateConnectionCounts(0, 1);

        assertThat(state.needsReconnect()).isTrue();
    }

    @Test
    void needsReconnect_returnsFalseAtStatusTimeoutBoundary() {
        var clock = new MutableClock(START);
        var state = new PontonWebSocketConnectionState(CONFIG, clock);
        state.markReceptionStarted();
        clock.set(START.plus(STATUS_TIMEOUT));

        assertThat(state.needsReconnect()).isFalse();
    }

    @Test
    void markRestarted_resetsConnectionCountsAndStartsStatusTimeoutAgain() {
        var clock = new MutableClock(START);
        var state = new PontonWebSocketConnectionState(CONFIG, clock);
        state.markReceptionStarted();
        state.updateConnectionCounts(1, 1);
        var restartedAt = START.plusSeconds(10);
        clock.set(restartedAt);

        state.markRestarted();
        clock.set(restartedAt.plus(STATUS_TIMEOUT).plusSeconds(1));

        assertThat(state.needsReconnect()).isTrue();
        assertThat(state.describe()).contains(
                "outbound=0",
                "inbound=0",
                "lastReceptionStartAt=" + restartedAt,
                "lastRestartAt=" + restartedAt,
                "lastConnectionStatusChangedAt=null"
        );
    }

    @Test
    void healthCheck_reportsWebSocketState() {
        var clock = new MutableClock(START);
        var state = new PontonWebSocketConnectionState(CONFIG, clock);
        state.markReceptionStarted();
        clock.set(START.plusSeconds(1));
        state.markAdapterStatusRequested();
        clock.set(START.plusSeconds(2));
        state.updateConnectionCounts(1, 1);

        var healthCheck = state.healthCheck();

        assertThat(healthCheck.name()).isEqualTo("adapterWebSocketConnection");
        assertThat(healthCheck.ok()).isTrue();
        assertThat(healthCheck.content()).contains(
                "started=true",
                "lastAdapterStatusRequestAt=" + START.plusSeconds(1)
        );
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
