package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32;

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
class EdaMasterData01p32InboundMessageFactoryTest {

    @Autowired
    private Jaxb2Marshaller marshaller;

    @Test
    void isActive_on_07_04_2024_returnsFalse() {
        // given
        var factory = new EdaMasterData01p32InboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2024, 4, 7));

        // then
        assertFalse(active);
    }

    @Test
    void isActive_on_08_04_2024_returnsTrue() {
        // given
        var factory = new EdaMasterData01p32InboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2024, 4, 8));

        // then
        assertTrue(active);
    }


    @Test
    void parseInputStream() throws IOException {
        // Given
        ClassLoader classLoader = EdaMasterData01p32InboundMessageFactory.class.getClassLoader();
        try (var inputStream = classLoader.getResourceAsStream("xsd/masterdata/_01p32/masterdata.xml")) {
            var factory = new EdaMasterData01p32InboundMessageFactory(marshaller);
            // When
            var result = factory.parseInputStream(inputStream);

            // Then
            assertAll(
                    () -> assertEquals("AT09999901234562022081314235688", result.conversationId()),
                    () -> assertEquals("AT099999012340000000000123456789", result.meteringPoint()),
                    () -> assertNotNull(result.originalMasterData())
            );
        }
    }
}
