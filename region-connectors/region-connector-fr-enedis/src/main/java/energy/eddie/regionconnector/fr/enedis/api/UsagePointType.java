// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.api;

import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Optional;

public enum UsagePointType {
    CONSUMPTION,
    PRODUCTION,
    CONSUMPTION_AND_PRODUCTION;

    public static Optional<UsagePointType> fromSegments(List<String> segments) {
        boolean consumption = false;
        boolean production = false;

        for (String segment : segments) {
            if (Strings.isEmpty(segment)) {
                continue;
            }

            if (segment.contains("C")) {
                consumption = true;
            }

            if (segment.contains("P")) {
                production = true;
            }
        }
        return fromBooleans(consumption, production);
    }

    public static Optional<UsagePointType> fromBooleans(boolean consumption, boolean production) {
        if (consumption && production) {
            return Optional.of(CONSUMPTION_AND_PRODUCTION);
        } else if (consumption) {
            return Optional.of(CONSUMPTION);
        } else if (production) {
            return Optional.of(PRODUCTION);
        } else {
            return Optional.empty();
        }
    }
}
