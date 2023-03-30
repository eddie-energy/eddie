package at.eda.xml.builders.helper;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;

public class DateTimeConverter {
    public static XMLGregorianCalendar dateTimeToXml(LocalDateTime dateTime) {
        try {
            if (dateTime == null) {
                throw new IllegalArgumentException("`dateTime` cannot be empty.");
            }

            return DatatypeFactory.newInstance().newXMLGregorianCalendar(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static XMLGregorianCalendar dateToXMl(LocalDate date) {
        try {
            if (date == null) {
                throw new IllegalArgumentException("`dateTime` cannot be empty.");
            }

            return DatatypeFactory.newInstance().newXMLGregorianCalendar(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalDateTime xmlToDateTime(XMLGregorianCalendar dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("`dateTime` cannot be empty.");
        }

        GregorianCalendar calendar = dateTime.toGregorianCalendar();
        ZonedDateTime zonedDateTime = calendar.toZonedDateTime();

        return zonedDateTime.toLocalDateTime();
    }
}
