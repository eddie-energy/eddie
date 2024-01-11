package energy.eddie.regionconnector.shared.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class StartOfDayZonedDateTimeDeserializerTest {
    private StartOfDayZonedDateTimeDeserializer deserializer;
    private JsonParser jsonParser;
    private DeserializationContext context;

    @BeforeEach
    void setup() {
        deserializer = new StartOfDayZonedDateTimeDeserializer();
        jsonParser = Mockito.mock(JsonParser.class);
        context = Mockito.mock(DeserializationContext.class);
    }

    @Test
    void givenDate_convertsToStartOfDay() throws IOException {
        // Given
        String dateString = "2024-01-05";
        when(jsonParser.getText()).thenReturn(dateString);
        ZonedDateTime expectedDate = ZonedDateTime.of(2024, 1, 5, 0, 0, 0, 0, ZoneId.of("UTC"));

        // When
        ZonedDateTime result = deserializer.deserialize(jsonParser, context);

        // Then
        assertEquals(expectedDate.toEpochSecond(), result.toEpochSecond());
    }

    @Test
    void givenDateTime_throwsDateTimeParseException() throws IOException {
        // Given
        String dateString = "2023-11-11 08:24:23";
        when(jsonParser.getText()).thenReturn(dateString);

        // When, Then
        assertThrows(DateTimeParseException.class, () -> deserializer.deserialize(jsonParser, context));
    }
}