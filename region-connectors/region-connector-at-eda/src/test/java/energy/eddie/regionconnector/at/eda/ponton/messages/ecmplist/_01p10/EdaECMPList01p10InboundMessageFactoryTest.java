// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p10;

import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MarshallerConfig.class)
class EdaECMPList01p10InboundMessageFactoryTest {
    @Autowired
    private Jaxb2Marshaller marshaller;

    @Test
    void parseInputStream_parsesMessagesAsExpected() throws IOException {
        // Given
        ClassLoader classLoader = EdaECMPList01p10InboundMessageFactory.class.getClassLoader();
        var inputStream = classLoader.getResourceAsStream("xsd/ecmplist/_01p10/ecmplist.xml");
        assert inputStream != null;
        var factory = new EdaECMPList01p10InboundMessageFactory(marshaller);

        // When
        var ecmpList = factory.parseInputStream(inputStream);
        inputStream.close();

        // Then
        assertAll(
                () -> assertEquals("AT000000000000000000000000000000000", ecmpList.messageId()),
                () -> assertEquals("CC000000000000000000000000000000000", ecmpList.conversationId()),
                () -> assertEquals("ATCC0000DYNAMCC000000000000000000", ecmpList.ecId())
        );
    }

    @ParameterizedTest
    @MethodSource("activeDates")
    void isActive_returnsTrue(LocalDate date) {
        // Given
        var factory = new EdaECMPList01p10InboundMessageFactory(marshaller);

        // When
        var active = factory.isActive(date);

        // Then
        assertTrue(active);
    }

    @ParameterizedTest
    @MethodSource("inactiveDates")
    void isActive_returnsFalse(LocalDate date) {
        // Given
        var factory = new EdaECMPList01p10InboundMessageFactory(marshaller);

        // When
        var active = factory.isActive(date);

        // Then
        assertFalse(active);
    }

    private static Stream<Arguments> inactiveDates() {
        return Stream.of(
                Arguments.of(LocalDate.of(2023, 1, 7)),
                Arguments.of(LocalDate.of(2024, 4, 7))
        );
    }

    private static Stream<Arguments> activeDates() {
        return Stream.of(
                Arguments.of(LocalDate.of(2026, 4, 13)),
                Arguments.of(LocalDate.of(2028, 6, 8)),
                Arguments.of(LocalDate.of(2030, 7, 9))
        );
    }
}