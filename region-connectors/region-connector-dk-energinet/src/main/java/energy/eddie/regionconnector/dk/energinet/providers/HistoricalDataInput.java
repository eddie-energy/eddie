// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers;

import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;

import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;

public record HistoricalDataInput(
        IdentifiableApiResponse source,
        HistoricalDataEmissionMetadata metadata
) {
    public HistoricalDataInput(IdentifiableApiResponse source) {
        this(source, new HistoricalDataEmissionMetadata());
    }
}
