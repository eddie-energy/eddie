// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;

import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;

public record HistoricalDataInput(
        IdentifiableMeterReading source,
        HistoricalDataEmissionMetadata metadata
) {
    public HistoricalDataInput(IdentifiableMeterReading source) {
        this(source, new HistoricalDataEmissionMetadata());
    }
}
