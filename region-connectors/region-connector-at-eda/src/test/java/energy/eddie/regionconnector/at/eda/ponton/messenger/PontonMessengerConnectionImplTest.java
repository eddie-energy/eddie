// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.MessengerConnection;
import de.ponton.xp.adapter.api.TransmissionException;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.InboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PontonMessengerConnectionImplTest {
    private static final PontonXPAdapterConfiguration CONFIG = new PontonXPAdapterConfiguration(
            "adapter-id",
            "adapter-version",
            "localhost",
            8080,
            "http://localhost:8081",
            "work",
            "user",
            "password"
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

    private PontonMessengerConnectionImpl connection() throws Exception {
        return new PontonMessengerConnectionImpl(
                CONFIG,
                workFolder.toFile(),
                mock(InboundMessageFactoryCollection.class),
                mock(OutboundMessageFactoryCollection.class),
                () -> new MessengerStatus(Map.of(), true),
                mock(MessengerMonitor.class)
        );
    }

    private MessengerConnection.MessengerConnectionBuilder messengerConnectionBuilderReturning(
            org.mockito.MockedStatic<MessengerConnection> messengerConnectionFactory
    ) {
        var builder = mock(MessengerConnection.MessengerConnectionBuilder.class, Answers.RETURNS_SELF);
        messengerConnectionFactory.when(MessengerConnection::newBuilder).thenReturn(builder);
        return builder;
    }
}
