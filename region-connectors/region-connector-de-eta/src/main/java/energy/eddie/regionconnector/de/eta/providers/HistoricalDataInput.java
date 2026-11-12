// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;

public record HistoricalDataInput(
        IdentifiableValidatedHistoricalData source,
        HistoricalDataEmissionMetadata metadata
) {
    public HistoricalDataInput(IdentifiableValidatedHistoricalData source) {
        this(source, new HistoricalDataEmissionMetadata());
    }
}
