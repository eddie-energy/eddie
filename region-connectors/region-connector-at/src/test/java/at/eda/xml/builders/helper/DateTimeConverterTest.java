package at.eda.xml.builders.helper;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateTimeConverterTest {
    @Test
    public void testDateTimeConverter() {
        // Correct test
        DateTimeConverter.dateToXml(LocalDate.now(ZoneId.of("Europe/Vienna")));
        DateTimeConverter.dateTimeToXml(LocalDateTime.now(ZoneId.of("Europe/Vienna")));
    }

    @Test
    public void testIllegalStateException() throws Throwable {
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
