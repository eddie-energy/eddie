package energy.eddie.core.converters.calculations;

import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;

import java.math.BigDecimal;

public record ScaledUnit(UnitOfMeasureTypeList unit, BigDecimal scale, EnergyProductTypeList energyProduct) {
}
