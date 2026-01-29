// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p33;

import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MarshallerConfig.class)
class EdaMasterData01p33InboundMessageFactoryTest {

    @Autowired
    private Jaxb2Marshaller marshaller;

    @Test
    void isActive_on_07_04_2024_returnsFalse() {
        // given
        var factory = new EdaMasterData01p33InboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2024, 4, 7));

        // then
        assertFalse(active);
    }

    @Test
    void isActive_on_07_04_2025_returnsTrue() {
        // given
        var factory = new EdaMasterData01p33InboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2025, 4, 7));

        // then
        assertTrue(active);
    }


    @Test
    void parseInputStream() throws IOException {
        // Given
        ClassLoader classLoader = EdaMasterData01p33InboundMessageFactory.class.getClassLoader();
        try (var inputStream = classLoader.getResourceAsStream("xsd/masterdata/_01p33/masterdata.xml")) {
            var factory = new EdaMasterData01p33InboundMessageFactory(marshaller);
            // When
            var result = factory.parseInputStream(inputStream);

            // Then
            assertAll(
                    () -> assertEquals("EP00000000000000000000", result.conversationId()),
                    () -> assertEquals("AT0000000000000000000000000000000", result.meteringPoint()),
                    () -> assertNotNull(result.originalMasterData())
            );
        }
    }
}