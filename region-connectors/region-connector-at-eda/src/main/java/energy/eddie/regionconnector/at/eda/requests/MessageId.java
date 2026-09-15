// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

public record MessageId(String address, ZonedDateTime dateTime) {
    public static final int MAX_LENGTH = 35;
    private static final String FORMAT = "%sT%s";

    public MessageId {
        requireNonNull(address);
        requireNonNull(dateTime);
        var length = formattedLength(address, dateTime);
        if (length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "EDA grouping ID length %d exceeds the maximum of %d characters".formatted(length, MAX_LENGTH)
            );
        }
    }

    @Override
    public String toString() {
        return FORMAT.formatted(address, dateTime.toInstant().toEpochMilli());
    }

    private static int formattedLength(String address, ZonedDateTime dateTime) {
        return FORMAT.formatted(address, dateTime.toInstant().toEpochMilli()).length();
    }
}
