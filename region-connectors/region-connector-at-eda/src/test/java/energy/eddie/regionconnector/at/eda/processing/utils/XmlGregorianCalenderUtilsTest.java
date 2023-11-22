package energy.eddie.regionconnector.at.eda.processing.utils;

import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XmlGregorianCalenderUtilsTest {

    @Test
    void toZonedDateTime_withMissingTimeZone_returnsUtc() {
        XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar();
        xmlGregorianCalendar.setYear(2021);
        xmlGregorianCalendar.setMonth(1);
        xmlGregorianCalendar.setDay(1);
        xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);

        var expected = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        var zonedDateTime = XmlGregorianCalenderUtils.toUtcZonedDateTime(xmlGregorianCalendar);

        assertEquals(expected, zonedDateTime);
        assertEquals(ZoneOffset.UTC, zonedDateTime.getZone());
    }

    @Test
    void toZonedDateTime_withTimeZone_returnsTimeZone() {
        var zoneId = ZoneId.of("Europe/Vienna");
        var expected = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        var vienna = expected.withZoneSameInstant(zoneId);
        GregorianCalendar gregorianCalendar = new GregorianCalendar(TimeZone.getTimeZone(zoneId));
        gregorianCalendar.setTimeInMillis(vienna.toInstant().toEpochMilli());
        XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(gregorianCalendar);


        var zonedDateTime = XmlGregorianCalenderUtils.toUtcZonedDateTime(xmlGregorianCalendar);

        assertEquals(expected, zonedDateTime);
        assertEquals(ZoneOffset.UTC, zonedDateTime.getZone());
    }
}