// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.utils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

public class EnedisDuration {
    /**
     * The minimum duration of the consent. Historical data needs end today or in the past, which would result in a
     * consent of a single day, which is too short for the customer to authorize and for the data to be fetched.
     */
    static final long MINIMUM_DAYS = 7;
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
        long days = Math.max(ChronoUnit.DAYS.between(now, end), MINIMUM_DAYS);
        return "P%sD".formatted(days);
    }
}
