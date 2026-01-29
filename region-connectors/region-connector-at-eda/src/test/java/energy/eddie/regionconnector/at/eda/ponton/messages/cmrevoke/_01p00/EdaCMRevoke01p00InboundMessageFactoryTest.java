// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p00;

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
class EdaCMRevoke01p00InboundMessageFactoryTest {
    @Autowired
    private Jaxb2Marshaller marshaller;

    public static Stream<Arguments> activeDates() {
        return Stream.of(
                Arguments.of(LocalDate.of(2023, 1, 7)),
                Arguments.of(LocalDate.of(2024, 4, 7)),
                Arguments.of(LocalDate.of(2025, 6, 8)),
                Arguments.of(LocalDate.of(2026, 4, 12))
        );
    }

    @ParameterizedTest
    @MethodSource("activeDates")
    void isActive_returnsTrue(LocalDate date) {
        // Given
        var factory = new EdaCMRevoke01p00InboundMessageFactory(marshaller);

        // When
        var active = factory.isActive(date);

        // Then
        assertTrue(active);
    }

    @Test
    void isActive_afterEndDate_returnsFalse() {
        // Given
        var afterEndDate = LocalDate.of(2026, 4, 13);
        var factory = new EdaCMRevoke01p00InboundMessageFactory(marshaller);

        // When
        var active = factory.isActive(afterEndDate);

        // Then
        assertFalse(active);
    }

    @Test
    void parseInputStream() throws IOException {
        // Given
        ClassLoader classLoader = EdaCMRevoke01p00InboundMessageFactoryTest.class.getClassLoader();
        try (var inputStream = classLoader.getResourceAsStream("xsd/cmrevoke/_01p00/cmrevoke.xml")) {
            var factory = new EdaCMRevoke01p00InboundMessageFactory(marshaller);
            // When
            var result = factory.parseInputStream(inputStream);

            // Then
            assertAll(
                    () -> assertEquals("ATXXXXXX20240403085709627V2YSCMHG", result.consentId()),
                    () -> assertEquals(LocalDate.of(2024, 4, 4), result.consentEnd()),
                    () -> assertEquals("ATXXXXXX00000000000000000XXXXXXXX", result.meteringPoint())
            );
        }
    }
}
