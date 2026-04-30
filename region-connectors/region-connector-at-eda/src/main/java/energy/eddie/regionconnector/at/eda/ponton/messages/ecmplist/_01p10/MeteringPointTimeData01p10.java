// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p10;

import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.MPTimeData;
import energy.eddie.api.agnostic.data.needs.EnergyDirection;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.MeteringPointTimeData;

import java.math.BigInteger;
import java.time.ZonedDateTime;

public record MeteringPointTimeData01p10(MPTimeData mpTimeData) implements MeteringPointTimeData {
    @Override
    public ZonedDateTime dateFrom() {
        return mpTimeData.getDateFrom().toGregorianCalendar().toZonedDateTime();
    }

    @Override
    public EnergyDirection energyDirection() {
        return switch (mpTimeData.getEnergyDirection()) {
            case CONSUMPTION -> EnergyDirection.CONSUMPTION;
            case GENERATION -> EnergyDirection.PRODUCTION;
        };
    }

    @Override
    public BigInteger ecPartFact() {
        return mpTimeData.getECPartFact().toBigInteger();
    }
}
