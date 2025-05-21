package energy.eddie.core.converters.v0_91_08;

import energy.eddie.cim.v0_91_08.StandardEnergyProductTypeList;
import energy.eddie.cim.v0_91_08.StandardUnitOfMeasureTypeList;

import java.math.BigDecimal;

record ScaledUnit(StandardUnitOfMeasureTypeList unit, BigDecimal scale, StandardEnergyProductTypeList energyProduct) {
}
