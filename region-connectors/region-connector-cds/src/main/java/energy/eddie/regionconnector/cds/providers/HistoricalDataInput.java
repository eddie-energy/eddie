// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers;

import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;

import energy.eddie.regionconnector.cds.providers.vhd.IdentifiableValidatedHistoricalData;

public record HistoricalDataInput(
        IdentifiableValidatedHistoricalData source,
        HistoricalDataEmissionMetadata metadata
) {
    public HistoricalDataInput(IdentifiableValidatedHistoricalData source) {
        this(source, new HistoricalDataEmissionMetadata());
    }
}
