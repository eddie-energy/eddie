package energy.eddie.regionconnector.at.eda.utils;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TruncatedZonedDateTimeTest {

    @Test
    void zonedDateTime() {
        // given
        TruncatedZonedDateTime truncatedZonedDateTime =
                new TruncatedZonedDateTime(ZonedDateTime.of(2023, 5, 5, 12, 35, 20, 20, ZoneId.systemDefault()));
        ZonedDateTime expectedValue = ZonedDateTime.of(2023, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());

        // when
        ZonedDateTime actualValue = truncatedZonedDateTime.zonedDateTime();

        // then
        assertEquals(expectedValue, actualValue);
    }
}