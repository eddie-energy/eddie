// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.xml.helper;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class DateTimeConverter {
    private DateTimeConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static XMLGregorianCalendar dateTimeToXml(LocalDateTime dateTime) {
        return DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(Objects.requireNonNull(dateTime).format(DateTimeFormatter.ISO_DATE_TIME));
    }

    public static XMLGregorianCalendar dateTimeToXml(ZonedDateTime dateTime) {
        return DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(Objects.requireNonNull(dateTime).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    public static XMLGregorianCalendar dateToXml(LocalDate date) {
        return DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(Objects.requireNonNull(date).format(DateTimeFormatter.ISO_DATE));
    }
}