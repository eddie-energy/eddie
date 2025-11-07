package energy.eddie.cim.serde;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultSerdeFactoryTest {

    @ParameterizedTest
    @ValueSource(strings = {"xml", "json"})
    void testCreate_returnsMessageSerde(String format) {
        // Given
        var factory = new DefaultSerdeFactory();

        // When
        // Then
        assertDoesNotThrow(() -> factory.create(format));
    }


    @Test
    void testCreate_throwsOnUnknownFormat() {
        // Given
        var factory = new DefaultSerdeFactory();

        // When
        // Then
        assertThrows(SerdeInitializationException.class, () -> factory.create("csv"));
    }
}