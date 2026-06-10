// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import org.jspecify.annotations.Nullable;

public record Measurement(@Nullable Reading offtake,
                          @Nullable Reading injection,
                          @Nullable Reading production,
                          @Nullable Reading auxiliary) {
    public boolean supportsInjection() {
        return injection != null;
    }

    public boolean supportsOfftake() {
        return offtake != null;
    }
}
