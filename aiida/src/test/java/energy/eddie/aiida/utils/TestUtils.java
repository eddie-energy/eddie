// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.utils;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {
    public static void verifyErrorLogStartsWith(String startString, LogCaptor logCaptor) {
        verifyErrorLogStartsWith(startString, logCaptor, null);
    }

    public static void verifyErrorLogStartsWith(String startString, LogCaptor logCaptor, Class<?> expectedExceptionClass) {
        verifyErrorLogStartsWith(startString, logCaptor, expectedExceptionClass, null);
    }

    public static void verifyErrorLogStartsWith(String startString, LogCaptor logCaptor, Class<?> expectedExceptionClass, String exceptionMessage) {
        var errorEvents = getErrorEvents(logCaptor);
        assertEquals(1, errorEvents.size());
        assertThat(errorEvents.get(0).getFormattedMessage()).startsWith(startString);

        if (expectedExceptionClass == null)
            return;

        var throwable = errorEvents.get(0).getThrowable();
        assertThat(throwable).isPresent();
        assertThat(errorEvents.get(0).getThrowable().orElseThrow())
                .isInstanceOf(expectedExceptionClass);

        if (exceptionMessage != null)
            assertThat(errorEvents.get(0).getThrowable().orElseThrow())
                    .hasMessage(exceptionMessage);
    }

    private static List<LogEvent> getErrorEvents(LogCaptor logCaptor) {
        return logCaptor.getLogEvents().stream().filter(logEvent -> logEvent.getLevel().equals("ERROR")).toList();
    }
}
