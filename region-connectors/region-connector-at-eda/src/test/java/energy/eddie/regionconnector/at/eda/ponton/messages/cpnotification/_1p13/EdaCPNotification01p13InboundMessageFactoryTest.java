// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification._1p13;

import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MarshallerConfig.class)
class EdaCPNotification01p13InboundMessageFactoryTest {

    @Autowired
    private Jaxb2Marshaller marshaller;

    public static Stream<Arguments> activeDates() {
        return Stream.of(
                Arguments.of(LocalDate.of(2023, 1, 7)),
                Arguments.of(LocalDate.of(2024, 4, 7)),
                Arguments.of(LocalDate.of(2025, 6, 8)),
                Arguments.of(LocalDate.of(2028, 7, 9))
        );
    }

    @ParameterizedTest
    @MethodSource("inputStreams")
    void parseInputStream_parsesMessagesAsExpected(
            InputStream inputStream,
            String conversationId,
            int responseCode
    ) throws IOException {
        // Given
        var factory = new EdaCPNotification01p13InboundMessageFactory(marshaller);

        // When
        var notification = factory.parseInputStream(inputStream);
        inputStream.close();

        // Then
        assertAll(
                () -> assertEquals(conversationId, notification.conversationId()),
                () -> assertEquals(conversationId, notification.originalMessageId()),
                () -> assertEquals(1, notification.responseCodes().size()),
                () -> assertEquals(responseCode, notification.responseCodes().getFirst())
        );
    }

    @ParameterizedTest
    @MethodSource("activeDates")
    void isActive_returnsTrue(LocalDate date) {
        // Given
        var factory = new EdaCPNotification01p13InboundMessageFactory(marshaller);

        // When
        var active = factory.isActive(date);

        // Then
        assertTrue(active);
    }

    private static Stream<Arguments> inputStreams() {
        ClassLoader classLoader = EdaCPNotification01p13InboundMessageFactoryTest.class.getClassLoader();
        var answer = classLoader.getResourceAsStream("xsd/cpnotification/_01p12/answer_pt.xml");
        var rejected = classLoader.getResourceAsStream("xsd/cpnotification/_01p12/rejected_pt.xml");

        return Stream.of(
                Arguments.of(answer, "EPXXXXXXT1729251297834", 70),
                Arguments.of(rejected, "EPXXXXXXT1729251281919", 82)
        );
    }
}
