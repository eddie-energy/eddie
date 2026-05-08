// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import org.jspecify.annotations.Nullable;

public record Reading(@Nullable Total total,
                      @Nullable Total day,
                      @Nullable Total night,
                      @Nullable Total reactive,
                      @Nullable Total inductive,
                      @Nullable Total capacitive) {
    public Reading(Total total) {
        this(total, null, null, null, null, null);
    }
}
