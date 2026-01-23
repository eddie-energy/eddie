// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.data.needs.EnergyType;

import java.util.Locale;

public record Coverage(EnergyType energyType, String countryCode) {

    @Override
    public String countryCode() {
        return countryCode.toLowerCase(Locale.ROOT);
    }
}
