// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.utils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

public class EnedisDuration {
    private final LocalDate end;
    private final Clock clock;

    public EnedisDuration(LocalDate end) {
        this(end, Clock.system(ZONE_ID_FR));
    }

    EnedisDuration(LocalDate end, Clock clock) {
        this.end = end;
        this.clock = clock;
    }

    @Override
    public String toString() {
        LocalDate now = LocalDate.now(clock);
        long days = ChronoUnit.DAYS.between(now, end);
        if (days <= 0) {
            days = 1; // minimum duration is 1 day
        }
        return "P%sD".formatted(days);
    }
}
