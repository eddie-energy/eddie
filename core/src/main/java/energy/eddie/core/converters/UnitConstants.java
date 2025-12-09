package energy.eddie.core.converters;

import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList;

import java.math.BigDecimal;
import java.util.List;

/**
 * Class to differentiate between the unit types, since they are grouped together.
 */
@SuppressWarnings("unused")
public class UnitConstants {
    public static final BigDecimal MINUTES_IN_HOUR = BigDecimal.valueOf(60);
    /* CIM v0.82 */
    public static final List<UnitOfMeasureTypeList> powerUnits = List.of(
            // Apparent Power
            UnitOfMeasureTypeList.MEGAVOLTAMPERE,
            UnitOfMeasureTypeList.KILOVOLT_AMPERE_REACTIVE,
            UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE,
            // Real Power
            UnitOfMeasureTypeList.KILOWATT,
            UnitOfMeasureTypeList.MEGAWATT,
            UnitOfMeasureTypeList.WATT,
            UnitOfMeasureTypeList.GIGAWATT
    );
    public static final List<UnitOfMeasureTypeList> voltageUnits = List.of(
            UnitOfMeasureTypeList.KILOVOLT
    );
    public static final List<UnitOfMeasureTypeList> currentUnits = List.of(
            UnitOfMeasureTypeList.AMPERE
    );
    public static final List<UnitOfMeasureTypeList> energyUnits = List.of(
            UnitOfMeasureTypeList.KILOWATT_HOUR,
            UnitOfMeasureTypeList.GIGAWATT_HOUR,
            UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
            UnitOfMeasureTypeList.MEGAWATT_HOURS
    );
    /* CIM v1.04 */
    public static final List<StandardUnitOfMeasureTypeList> POWER_UNITS = List.of(
            // Apparent Power
            StandardUnitOfMeasureTypeList.MEGAVOLTAMPERE,
            StandardUnitOfMeasureTypeList.KILOVOLT_AMPERE_REACTIVE,
            StandardUnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE,
            // Real Power
            StandardUnitOfMeasureTypeList.KILOWATT,
            StandardUnitOfMeasureTypeList.MEGAWATT,
            StandardUnitOfMeasureTypeList.WATT,
            StandardUnitOfMeasureTypeList.GIGAWATT
    ) ;
    public static final List<StandardUnitOfMeasureTypeList> ENERGY_UNITS = List.of(
            StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
            StandardUnitOfMeasureTypeList.GIGAWATT_HOUR,
            StandardUnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
            StandardUnitOfMeasureTypeList.MEGAWATT_HOURS
    );

    private UnitConstants() {
        throw new IllegalStateException("Utility class");
    }
}
