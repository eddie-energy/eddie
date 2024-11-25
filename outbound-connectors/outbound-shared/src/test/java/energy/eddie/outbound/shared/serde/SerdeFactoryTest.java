package energy.eddie.outbound.shared.serde;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SerdeFactoryTest {

    @Test
    void testGetInstance_returnsNotNull() {
        // Given
        // When
        var res = SerdeFactory.getInstance();

        // Then
        assertNotNull(res);
    }
}