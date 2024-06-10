package energy.eddie.core.converters;

import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;

import java.util.List;

/**
 * Class to differentiate between the unit types, since they are grouped together.
 */
@SuppressWarnings("unused")
public class UnitConstants {
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

    private UnitConstants() {
        throw new IllegalStateException("Utility class");
    }
}
