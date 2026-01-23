// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.converters.calculations;

import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;

import java.math.BigDecimal;

public record ScaledUnit(UnitOfMeasureTypeList unit, BigDecimal scale, EnergyProductTypeList energyProduct) {
}
