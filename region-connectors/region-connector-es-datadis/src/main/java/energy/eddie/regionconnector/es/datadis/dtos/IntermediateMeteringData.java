// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record IntermediateMeteringData(
        List<MeteringData> meteringData,
        LocalDate start,
        LocalDate end
) {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static Mono<IntermediateMeteringData> fromMeteringData(List<MeteringData> meteringData) {
        if (meteringData == null || meteringData.isEmpty()) {
            return Mono.empty();
        }
        MeteringData first = meteringData.getFirst();
        MeteringData last = meteringData.getLast();

        LocalDate start = parseDate(first.date(), first.time());
        LocalDate end = parseDate(last.date(), last.time());

        return Mono.just(new IntermediateMeteringData(meteringData, start, end));
    }

    private static LocalDate parseDate(LocalDate date, String timeString) {
        LocalTime time = LocalTime.parse(timeString, TIME_FORMAT);

        // Datadis API returns the time 24:00 for the next day, this is parsed as LocalTime.MIN, but we need to add a day to the date
        if (time.equals(LocalTime.MIN)) {
            date = date.plusDays(1);
        }
        return date;
    }
}
