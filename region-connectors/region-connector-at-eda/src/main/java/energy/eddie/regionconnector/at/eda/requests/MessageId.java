// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

public record MessageId(String address, ZonedDateTime dateTime) {
    private static final String FORMAT = "%sT%s";

    public MessageId {
        requireNonNull(address);
        requireNonNull(dateTime);
    }

    @Override
    public String toString() {
        return FORMAT.formatted(address, dateTime.toInstant().toEpochMilli());
    }
}
