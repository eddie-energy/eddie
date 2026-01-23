// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMessageSerdeTest {
    @Test
    void testSerialize_serializesMessage() throws SerializationException {
        // Given
        var serde = new JsonMessageSerde();
        var message = Map.of("key", "value");
        //language=json
        var expected = "{\"key\":\"value\"}";

        // When
        var res = serde.serialize(message);

        // Then
        assertEquals(expected, new String(res, StandardCharsets.UTF_8));
    }

    @Test
    void testDeserialize_deserializesMessage() throws DeserializationException {
        // Given
        var serde = new JsonMessageSerde();
        //language=JSON
        var message = "{\"key\":\"value\"}";

        // When
        var res = serde.deserialize(message.getBytes(StandardCharsets.UTF_8), Map.class);

        // Then
        assertEquals(Map.of("key", "value"), res);
    }

    @Test
    void testDeserialize_throwsOnInvalidJson() {
        // Given
        var serde = new JsonMessageSerde();

        // When
        // Then
        assertThrows(DeserializationException.class,
                     () -> serde.deserialize("jfjklasfjklsa".getBytes(StandardCharsets.UTF_8), Map.class));
    }
}