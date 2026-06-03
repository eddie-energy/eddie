// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.extensions;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

public class CimDateAdapter extends XmlAdapter<String, ZonedDateTime> {
    @Override
    public ZonedDateTime unmarshal(String stringValue) {
        if (stringValue == null) {
            return null;
        }
        try {
            TemporalAccessor parsed = DateTimeFormatter.ISO_DATE.parse(stringValue);
            LocalDate date = LocalDate.from(parsed);
            ZoneOffset offset = ZoneOffset.from(parsed);
            return date.atStartOfDay()
                       .atOffset(offset)
                       .toZonedDateTime();
        } catch (Exception e) {
            return ZonedDateTime.parse(stringValue, ISO_DATE_TIME);
        }
    }

    @Override
    public String marshal(ZonedDateTime value) {
        if (value == null) {
            return null;
        }
        return ISO_DATE.format(value.withZoneSameInstant(ZoneOffset.UTC));
    }
}
