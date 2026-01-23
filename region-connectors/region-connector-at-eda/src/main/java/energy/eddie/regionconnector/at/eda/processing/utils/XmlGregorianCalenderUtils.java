// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.processing.utils;

import javax.xml.datatype.DatatypeConstants;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class XmlGregorianCalenderUtils {

    private XmlGregorianCalenderUtils() {
    }

    /**
     * Converts a XMLGregorianCalendar to a ZonedDateTime with UTC timezone.
     * If the XMLGregorianCalendar has no timezone set, it will be assumed to be UTC.
     *
     * @param xmlGregorianCalendar XMLGregorianCalendar to convert to ZonedDateTime
     * @return ZonedDateTime representation of the XMLGregorianCalendar
     */
    public static ZonedDateTime toUtcZonedDateTime(javax.xml.datatype.XMLGregorianCalendar xmlGregorianCalendar) {
        if (xmlGregorianCalendar.getTimezone() == DatatypeConstants.FIELD_UNDEFINED) {
            xmlGregorianCalendar.setTimezone(0);
        }

        return xmlGregorianCalendar.toGregorianCalendar().toZonedDateTime().withZoneSameInstant(ZoneOffset.UTC);
    }
}