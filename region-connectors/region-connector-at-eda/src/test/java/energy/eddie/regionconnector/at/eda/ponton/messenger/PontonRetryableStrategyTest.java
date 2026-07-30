// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.TransmissionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PontonRetryableStrategyTest {
    @Mock
    private MessengerHealth healthApi;
    @InjectMocks
    private PontonRetryableStrategy retryStrategy;

    @ParameterizedTest
    @MethodSource("exceptionsWithIOException")
    void isRetryable_shouldReturnTrue_whenIOExceptionInChain_andMessengerIsOk(Exception exception) {
        // Given
        when(healthApi.messengerStatus()).thenReturn(new MessengerStatus(Map.of(), true));

        // When
        var result = retryStrategy.isRetryable(exception);

        // Then
        assertTrue(result);
    }

    @ParameterizedTest
    @MethodSource("exceptionsWithoutIOException")
    void isRetryable_shouldReturnFalse_whenNoIOExceptionInChain(Exception exception) {
        // When
        var result = retryStrategy.isRetryable(exception);

        // Then
        assertFalse(result);
    }

    @Test
    void isRetryable_shouldReturnFalse_whenIOExceptionInChain_butMessengerIsDown() {
        // Given
        when(healthApi.messengerStatus()).thenReturn(new MessengerStatus(Map.of(), false));

        // When
        var result = retryStrategy.isRetryable(new IOException("test"));

        // Then
        assertFalse(result);
    }

    static Stream<Arguments> exceptionsWithIOException() {
        return Stream.of(
                Arguments.of(new IOException("test")),
                Arguments.of(new TransmissionException(
                        "could not transmit message",
                        new ExecutionException(
                                new ConnectionException("could not establish connection",
                                                        new SocketException("Connection reset"))
                        )
                ))
        );
    }

    static Stream<Arguments> exceptionsWithoutIOException() {
        return Stream.of(
                Arguments.of(new TransmissionException("some error", new NullPointerException("test"))),
                Arguments.of(new TransmissionException(
                        "could not transmit message",
                        new ConnectionException("some error", new IllegalArgumentException("bad arg"))
                )),
                Arguments.of(new NullPointerException("test"))
        );
    }
}