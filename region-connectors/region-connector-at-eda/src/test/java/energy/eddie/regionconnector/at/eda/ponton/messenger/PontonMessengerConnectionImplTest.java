// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import de.ponton.xp.adapter.api.AdapterStatusRequestHandler;
import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.ConnectionStatusChangeHandler;
import de.ponton.xp.adapter.api.MessengerConnection;
import de.ponton.xp.adapter.api.TransmissionException;
import de.ponton.xp.adapter.api.domainvalues.MessengerInstance;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.InboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PontonMessengerConnectionImplTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneOffset.UTC);
    private static final PontonXPAdapterConfiguration CONFIG = new PontonXPAdapterConfiguration(
            "adapter-id",
            "adapter-version",
            "localhost",
            8080,
            "http://localhost:8081",
            "work",
            "user",
            "password",
            1,
            1,
            Duration.ofMinutes(1),
            Duration.ofMinutes(2)
    );

    @TempDir
    private Path workFolder;

    @Test
    void close_closesCurrentMessengerConnectionOnlyOnce() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var messengerConnection = mock(MessengerConnection.class);
            when(builder.build()).thenReturn(messengerConnection);
            var connection = connection();

            connection.close();
            connection.close();

            verify(messengerConnection).close();
            verify(builder).build();
        }
    }

    @Test
    void sendMessage_afterCloseThrowsAndDoesNotReconnect() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var messengerConnection = mock(MessengerConnection.class);
            when(builder.build()).thenReturn(messengerConnection);
            var connection = connection();
            connection.close();

            assertThrows(ConnectionException.class, () -> connection.sendMessage(mock(OutboundMessage.class)));

            verify(messengerConnection).close();
            verify(messengerConnection, never()).startReception();
            verify(builder).build();
        }
    }

    @Test
    void sendMessage_reconnectsAndRetriesWhenOpenAndSendFailsWithRetryableException() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var firstMessengerConnection = mock(MessengerConnection.class);
            var secondMessengerConnection = mock(MessengerConnection.class);
            var outboundMessage = mock(OutboundMessage.class);
            when(builder.build()).thenReturn(firstMessengerConnection, secondMessengerConnection);
            doThrow(new TransmissionException("send failed", new IOException("socket closed")))
                    .when(firstMessengerConnection)
                    .sendMessage(outboundMessage);
            var connection = connection();

            connection.sendMessage(outboundMessage);

            verify(firstMessengerConnection).close();
            verify(secondMessengerConnection).startReception();
            verify(secondMessengerConnection).sendMessage(outboundMessage);
            verify(builder, times(2)).build();
        }
    }

    @Test
    void start_startsReceptionAndExposesConfiguredWatchdogInterval() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var messengerConnection = mock(MessengerConnection.class);
            when(builder.build()).thenReturn(messengerConnection);
            var connection = connection();

            connection.start();

            verify(messengerConnection).startReception();
            assertThat(connection.connectionWatchdogInterval()).isEqualTo(CONFIG.connectionWatchdogInterval());
        }
    }

    @Test
    void sendMessage_sendsWithoutReconnectWhenFirstAttemptSucceeds() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var messengerConnection = mock(MessengerConnection.class);
            var outboundMessage = mock(OutboundMessage.class);
            when(builder.build()).thenReturn(messengerConnection);
            var connection = connection();

            connection.sendMessage(outboundMessage);

            verify(messengerConnection).sendMessage(outboundMessage);
            verify(messengerConnection, never()).close();
            verify(builder).build();
        }
    }

    @Test
    void sendMessage_wrapsNonRetryableFailureWithoutReconnect() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var messengerConnection = mock(MessengerConnection.class);
            var outboundMessage = mock(OutboundMessage.class);
            when(builder.build()).thenReturn(messengerConnection);
            doThrow(new IllegalArgumentException("invalid message"))
                    .when(messengerConnection)
                    .sendMessage(outboundMessage);
            var connection = connection();

            assertThrows(ConnectionException.class, () -> connection.sendMessage(outboundMessage));

            verify(messengerConnection, never()).close();
            verify(builder).build();
        }
    }

    @Test
    void messengerStatus_combinesMessengerAndWebSocketHealth() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var messengerConnection = mock(MessengerConnection.class);
            when(builder.build()).thenReturn(messengerConnection);
            var healthApi = mock(MessengerHealth.class);
            when(healthApi.messengerStatus()).thenReturn(new MessengerStatus(Map.of(), true));
            var connection = connection(healthApi, mock(MessengerMonitor.class), CLOCK);

            var status = connection.messengerStatus();

            assertThat(status.ok()).isTrue();
            assertThat(status.healthChecks()).containsKey("adapterWebSocketConnection");
        }
    }

    @Test
    void reconnectIfConnectionStale_rebuildsAndStartsConnectionAfterTimeout() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var firstMessengerConnection = mock(MessengerConnection.class);
            var secondMessengerConnection = mock(MessengerConnection.class);
            when(builder.build()).thenReturn(firstMessengerConnection, secondMessengerConnection);
            var clock = new MutableClock(CLOCK.instant());
            var connection = connection(mock(MessengerHealth.class), mock(MessengerMonitor.class), clock);
            connection.start();
            clock.advance(CONFIG.connectionStatusTimeout().plusSeconds(1));

            connection.reconnectIfConnectionStale();

            verify(firstMessengerConnection).close();
            verify(secondMessengerConnection).startReception();
            verify(builder, times(2)).build();
        }
    }

    @Test
    void reconnectIfConnectionStale_doesNothingAfterClose() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var messengerConnection = mock(MessengerConnection.class);
            when(builder.build()).thenReturn(messengerConnection);
            var connection = connection();
            connection.close();

            connection.reconnectIfConnectionStale();

            verify(messengerConnection).close();
            verify(builder).build();
        }
    }

    @Test
    void resendFailedMessage_delegatesToMonitor() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var messengerConnection = mock(MessengerConnection.class);
            when(builder.build()).thenReturn(messengerConnection);
            var monitor = mock(MessengerMonitor.class);
            var connection = connection(mock(MessengerHealth.class), monitor, CLOCK);
            var date = ZonedDateTime.parse("2026-08-13T12:00:00Z");

            connection.resendFailedMessage(date, "message-id");

            verify(monitor).resendFailedMessage(date, "message-id");
        }
    }

    @Test
    void registeredStatusCallbacks_updateAndReportWebSocketState() throws Exception {
        try (var messengerConnectionFactory = mockStatic(MessengerConnection.class)) {
            var builder = messengerConnectionBuilderReturning(messengerConnectionFactory);
            var messengerConnection = mock(MessengerConnection.class);
            when(builder.build()).thenReturn(messengerConnection);
            var connectionStatusHandler = ArgumentCaptor.forClass(ConnectionStatusChangeHandler.class);
            var adapterStatusHandler = ArgumentCaptor.forClass(AdapterStatusRequestHandler.class);
            connection();
            verify(builder).onConnectionStatusChanged(connectionStatusHandler.capture());
            verify(builder).onAdapterStatusRequest(adapterStatusHandler.capture());
            var messengerInstance = mock(MessengerInstance.class);

            connectionStatusHandler.getValue().connectionCountChanged(messengerInstance, 0, 0, 0);
            connectionStatusHandler.getValue().connectionCountChanged(messengerInstance, 1, 1, 0);
            var adapterStatus = adapterStatusHandler.getValue().onAdapterStatusRequest();

            assertThat(adapterStatus).contains(
                    CONFIG.adapterId(),
                    CONFIG.adapterVersion(),
                    "outbound=1",
                    "inbound=1"
            );
        }
    }

    private PontonMessengerConnectionImpl connection() throws Exception {
        return connection(
                () -> new MessengerStatus(Map.of(), true),
                mock(MessengerMonitor.class),
                CLOCK
        );
    }

    private PontonMessengerConnectionImpl connection(
            MessengerHealth healthApi,
            MessengerMonitor monitor,
            Clock clock
    ) throws Exception {
        return new PontonMessengerConnectionImpl(
                CONFIG,
                workFolder.toFile(),
                mock(InboundMessageFactoryCollection.class),
                mock(OutboundMessageFactoryCollection.class),
                healthApi,
                monitor,
                clock
        );
    }

    private MessengerConnection.MessengerConnectionBuilder messengerConnectionBuilderReturning(
            org.mockito.MockedStatic<MessengerConnection> messengerConnectionFactory
    ) {
        var builder = mock(MessengerConnection.MessengerConnectionBuilder.class, Answers.RETURNS_SELF);
        messengerConnectionFactory.when(MessengerConnection::newBuilder).thenReturn(builder);
        return builder;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
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
