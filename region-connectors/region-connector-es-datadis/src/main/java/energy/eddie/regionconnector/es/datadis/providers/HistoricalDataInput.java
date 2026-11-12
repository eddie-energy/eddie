// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers;

import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;

import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;

public record HistoricalDataInput(
        IdentifiableMeteringData source,
        HistoricalDataEmissionMetadata metadata
) {
    public HistoricalDataInput(IdentifiableMeteringData source) {
        this(source, new HistoricalDataEmissionMetadata());
    }
}
