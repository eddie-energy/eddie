// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.agnostic;

import energy.eddie.api.agnostic.Granularity;

public enum EnedisResolution {
    PT10M(Granularity.PT10M),
    PT15M(Granularity.PT15M),
    PT30M(Granularity.PT30M),
    PT60M(Granularity.PT1H),
    P1D(Granularity.P1D);

    private final Granularity granularity;

    EnedisResolution(Granularity iso8601) {
        this.granularity = iso8601;
    }

    public Granularity granularity() {
        return granularity;
    }
}
