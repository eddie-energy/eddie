// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.converters.v1_04;


import energy.eddie.cim.v1_04.StandardEnergyProductTypeList;
import energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList;

import java.math.BigDecimal;

record ScaledUnit(StandardUnitOfMeasureTypeList unit, BigDecimal scale, StandardEnergyProductTypeList energyProduct) {
}
