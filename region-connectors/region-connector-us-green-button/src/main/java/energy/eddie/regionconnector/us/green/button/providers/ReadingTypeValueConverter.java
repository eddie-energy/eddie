package energy.eddie.regionconnector.us.green.button.providers;

import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList;
import org.naesb.espi.ReadingType;

import java.util.Map;

public class ReadingTypeValueConverter<T> {
    private static final int NONE_TO_MEGA = -6;
    private static final int NONE_TO_KILO = -5;
    private static final int NONE_TO_HECTO = -2;
    private final int scale;
    private final String uom;
    private final Map<String, MeasurementScale<T>> unitMap;

    private static final Map<String, MeasurementScale<StandardUnitOfMeasureTypeList>> v104MAP = Map.ofEntries(
            Map.entry("61", new MeasurementScale<>(StandardUnitOfMeasureTypeList.MEGAVOLTAMPERE, NONE_TO_MEGA)),
            Map.entry("38", new MeasurementScale<>(StandardUnitOfMeasureTypeList.WATT, 0)),
            Map.entry("63", new MeasurementScale<>(StandardUnitOfMeasureTypeList.KILOVOLT_AMPERE_REACTIVE, NONE_TO_KILO)),
            Map.entry("71", new MeasurementScale<>(StandardUnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS, NONE_TO_MEGA)),
            Map.entry("72", new MeasurementScale<>(StandardUnitOfMeasureTypeList.KILOWATT_HOUR, NONE_TO_KILO)),
            Map.entry("73", new MeasurementScale<>(StandardUnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS, NONE_TO_MEGA)),
            Map.entry("29", new MeasurementScale<>(StandardUnitOfMeasureTypeList.KILOVOLT, NONE_TO_KILO)),
            Map.entry("5", new MeasurementScale<>(StandardUnitOfMeasureTypeList.AMPERE, 0)),
            Map.entry("23", new MeasurementScale<>(StandardUnitOfMeasureTypeList.CELSIUS, 0)),
            Map.entry("9", new MeasurementScale<>(StandardUnitOfMeasureTypeList.DEGREE_UNIT_OF_ANGLE, 0)),
            Map.entry("39", new MeasurementScale<>(StandardUnitOfMeasureTypeList.HECTOPASCAL, NONE_TO_HECTO)),
            Map.entry("2", new MeasurementScale<>(StandardUnitOfMeasureTypeList.METRE, 0)),
            Map.entry("42", new MeasurementScale<>(StandardUnitOfMeasureTypeList.CUBIC_METRE, 0)),
            Map.entry("6", new MeasurementScale<>(StandardUnitOfMeasureTypeList.K_KELVIN, 0)),
            Map.entry("45", new MeasurementScale<>(StandardUnitOfMeasureTypeList.CUBIC_METRES_PER_SECOND, 0))
    );

    private static final Map<String, MeasurementScale<UnitOfMeasureTypeList>> v082MAP = Map.ofEntries(
            Map.entry("61", new MeasurementScale<>(UnitOfMeasureTypeList.MEGAVOLTAMPERE, NONE_TO_MEGA)),
            Map.entry("38", new MeasurementScale<>(UnitOfMeasureTypeList.WATT, 0)),
            Map.entry("63", new MeasurementScale<>(UnitOfMeasureTypeList.KILOVOLT_AMPERE_REACTIVE, NONE_TO_KILO)),
            Map.entry("71", new MeasurementScale<>(UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS, NONE_TO_MEGA)),
            Map.entry("72", new MeasurementScale<>(UnitOfMeasureTypeList.KILOWATT_HOUR, NONE_TO_KILO)),
            Map.entry("73", new MeasurementScale<>(UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS, NONE_TO_MEGA)),
            Map.entry("29", new MeasurementScale<>(UnitOfMeasureTypeList.KILOVOLT, NONE_TO_KILO)),
            Map.entry("5", new MeasurementScale<>(UnitOfMeasureTypeList.AMPERE, 0)),
            Map.entry("23", new MeasurementScale<>(UnitOfMeasureTypeList.CELSIUS, 0)),
            Map.entry("9", new MeasurementScale<>(UnitOfMeasureTypeList.DEGREE_UNIT_OF_ANGLE, 0)),
            Map.entry("39", new MeasurementScale<>(UnitOfMeasureTypeList.HECTOPASCAL, NONE_TO_HECTO)),
            Map.entry("2", new MeasurementScale<>(UnitOfMeasureTypeList.METRE, 0)),
            Map.entry("42", new MeasurementScale<>(UnitOfMeasureTypeList.CUBIC_METRE, 0)),
            Map.entry("6", new MeasurementScale<>(UnitOfMeasureTypeList.K_KELVIN, 0)),
            Map.entry("45", new MeasurementScale<>(UnitOfMeasureTypeList.CUBIC_METRES_PER_SECOND, 0))
    );


    private ReadingTypeValueConverter(ReadingType readingType, Map<String, MeasurementScale<T>> unitMap) {
        this(Integer.parseInt(readingType.getPowerOfTenMultiplier()), readingType.getUom(), unitMap);
    }

    private ReadingTypeValueConverter(int scale, String uom, Map<String, MeasurementScale<T>> unitMap) {
        this.scale = scale;
        this.uom = uom;
        this.unitMap = unitMap;
    }

    public static ReadingTypeValueConverter<StandardUnitOfMeasureTypeList> v104UnitOfMeasureTypeList(ReadingType rt) {
        return new ReadingTypeValueConverter<>(rt, v104MAP);
    }

    public static ReadingTypeValueConverter<StandardUnitOfMeasureTypeList> v104UnitOfMeasureTypeList(int scale, String vom) {
        return new ReadingTypeValueConverter<>(scale, vom, v104MAP);
    }

    public static ReadingTypeValueConverter<UnitOfMeasureTypeList> v082UnitOfMeasureTypeList(ReadingType rt) {
        return new ReadingTypeValueConverter<>(rt, v082MAP);
    }

    public static ReadingTypeValueConverter<UnitOfMeasureTypeList> v082UnitOfMeasureTypeList(int scale, String vom) {
        return new ReadingTypeValueConverter<>(scale, vom, v082MAP);
    }

    /**
     * Converts the value provided by the green button API to CIM units.
     *
     * @return CIM unit
     * @see <a href="https://utilityapi.com/docs/greenbutton/xml#ReadingType">ReadingType</a>
     * @see <a href="https://zepben.github.io/evolve/docs/cim/ewb/IEC61970/Base/Domain/UnitSymbol/">UnitSymbol</a>
     */
    public MeasurementScale<T> scale() throws UnsupportedUnitException {
        MeasurementScale<T> mapping = unitMap.get(uom);
        if (mapping == null) {
            throw new UnsupportedUnitException(uom);
        }
        return new MeasurementScale<>(mapping.unit(), scale + mapping.scale());
    }

}
