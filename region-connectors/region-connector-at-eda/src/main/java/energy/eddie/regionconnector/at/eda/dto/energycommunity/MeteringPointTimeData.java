// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.energycommunity;

import energy.eddie.api.agnostic.data.needs.EnergyDirection;

import java.math.BigInteger;
import java.time.ZonedDateTime;

public interface MeteringPointTimeData {
    ZonedDateTime dateFrom();

    EnergyDirection energyDirection();

    BigInteger ecPartFact();
}
