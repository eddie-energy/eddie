package energy.eddie.regionconnector.at.eda.xml.helper;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DateTimeConverterTest {
    @Test
    void testDateTimeConverter() {
        // Correct test
        assertNotNull(DateTimeConverter.dateToXml(LocalDate.now(ZoneId.of("Europe/Vienna"))));
        assertNotNull(DateTimeConverter.dateTimeToXml(LocalDateTime.now(ZoneId.of("Europe/Vienna"))));
    }

    @Test
    void testIllegalStateException() throws Throwable {
        // Create Object of Utility class
        try {
            Constructor<DateTimeConverter> constructor = DateTimeConverter.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalStateException) {
                assertEquals("Utility class", e.getCause().getMessage());
            } else {
                throw e.getCause();
            }
        }
    }
}