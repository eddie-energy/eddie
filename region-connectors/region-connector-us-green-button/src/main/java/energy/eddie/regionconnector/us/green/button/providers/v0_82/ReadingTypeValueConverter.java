package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import org.naesb.espi.ReadingType;

class ReadingTypeValueConverter {
    private static final int NONE_TO_MEGA = -6;
    private static final int NONE_TO_KILO = -5;
    private static final int NONE_TO_HECTO = -2;
    private final int scale;
    private final String uom;

    ReadingTypeValueConverter(ReadingType readingType) {
        this(Integer.parseInt(readingType.getPowerOfTenMultiplier()), readingType.getUom());
    }

    ReadingTypeValueConverter(int scale, String uom) {
        this.scale = scale;
        this.uom = uom;
    }

    /**
     * Converts the value provided by the green button API to CIM units.
     *
     * @return CIM unit
     * @see <a href="https://utilityapi.com/docs/greenbutton/xml#ReadingType">ReadingType</a>
     * @see <a href="https://zepben.github.io/evolve/docs/cim/ewb/IEC61970/Base/Domain/UnitSymbol/">UnitSymbol</a>
     */
    @SuppressWarnings("java:S1479")
    MeasurementScale scale() throws UnsupportedUnitException {
        return switch (uom) {
            // Volt Ampere
            case "61" -> new MeasurementScale(UnitOfMeasureTypeList.MEGAVOLTAMPERE, calculateScale(NONE_TO_MEGA));
            // Watt
            case "38" -> new MeasurementScale(UnitOfMeasureTypeList.WATT, scale);
            // Volt Ampere Reactive
            case "63" ->
                    new MeasurementScale(UnitOfMeasureTypeList.KILOVOLT_AMPERE_REACTIVE, calculateScale(NONE_TO_KILO));
            // Volt Ampere Hours
            case "71" -> new MeasurementScale(UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
                                              calculateScale(NONE_TO_MEGA));
            // Watt Hours
            case "72" -> new MeasurementScale(UnitOfMeasureTypeList.KILOWATT_HOUR, calculateScale(NONE_TO_KILO));
            // Volt Ampere Reactive Hours
            case "73" -> new MeasurementScale(UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
                                              calculateScale(NONE_TO_MEGA));
            // Volt
            case "29" -> new MeasurementScale(UnitOfMeasureTypeList.KILOVOLT, calculateScale(NONE_TO_KILO));
            // Ohm
            case "30" -> throw new UnsupportedUnitException("Ohm");
            // Ampere
            case "5" -> new MeasurementScale(UnitOfMeasureTypeList.AMPERE, scale);
            // Farad
            case "25" -> throw new UnsupportedUnitException("Farad");
            // Henry
            case "28" -> throw new UnsupportedUnitException("Henry");
            // Celsius
            case "23" -> new MeasurementScale(UnitOfMeasureTypeList.CELSIUS, scale);
            // Second
            case "27" -> throw new UnsupportedUnitException("Second");
            // Minute
            case "159" -> throw new UnsupportedUnitException("Minute");
            // Hour
            case "160" -> throw new UnsupportedUnitException("Hour");
            // Degree
            case "9" -> new MeasurementScale(UnitOfMeasureTypeList.DEGREE_UNIT_OF_ANGLE, scale);
            // Radian
            case "10" -> throw new UnsupportedUnitException("Radian");
            // Joule
            case "31" -> throw new UnsupportedUnitException("Joule");
            // Newton
            case "32" -> throw new UnsupportedUnitException("Newton");
            // Siemens
            case "53" -> throw new UnsupportedUnitException("Siemens");
            // None
            case "0" -> throw new UnsupportedUnitException("None");
            // Hertz
            case "33" -> throw new UnsupportedUnitException("Hertz");
            // Gauss
            case "3" -> throw new UnsupportedUnitException("Gauss");
            // Pascal
            case "39" -> new MeasurementScale(UnitOfMeasureTypeList.HECTOPASCAL, calculateScale(NONE_TO_HECTO));
            // Metre
            case "2" -> new MeasurementScale(UnitOfMeasureTypeList.METRE, scale);
            // Square Metre
            case "41" -> throw new UnsupportedUnitException("Square Metre");
            // Cubic Metre
            case "42" -> new MeasurementScale(UnitOfMeasureTypeList.CUBIC_METRE, scale);
            // Amperes Squared
            case "69" -> throw new UnsupportedUnitException("Amperes Squared");
            // Amperes Squared Hours
            case "105" -> throw new UnsupportedUnitException("Amperes Squared Hours");
            // Amperes Squared Seconds
            case "70" -> throw new UnsupportedUnitException("Amperes Squared Seconds");
            // Ampere Hours
            case "106" -> throw new UnsupportedUnitException("Ampere Hours");
            // ratio of amperages
            case "152" -> throw new UnsupportedUnitException("ratio of amperages");
            // Amperes per metre
            case "103" -> throw new UnsupportedUnitException("Amperes per metre");
            // Ampere Seconds
            case "68" -> throw new UnsupportedUnitException("Ampere Seconds");
            // bar
            case "79" -> throw new UnsupportedUnitException("bar");
            // Bel-mW
            case "113" -> throw new UnsupportedUnitException("Bel-mW");
            // Becquerel
            case "22" -> throw new UnsupportedUnitException("Becquerel");
            // British Thermal Unit
            case "132" -> throw new UnsupportedUnitException("British Thermal Unit");
            // British Thermal Unit Hours
            case "133" -> throw new UnsupportedUnitException("British Thermal Unit Hours");
            // Candela
            case "8" -> throw new UnsupportedUnitException("Candela");
            // Charcoal
            case "76" -> throw new UnsupportedUnitException("Charcoal");
            // Hertz per Second
            case "75" -> throw new UnsupportedUnitException("Hertz per Second");
            // code
            case "114" -> throw new UnsupportedUnitException("code");
            // cosine of theta
            case "65" -> throw new UnsupportedUnitException("Cosine of Theta");
            // count
            case "111" -> throw new UnsupportedUnitException("count");
            // Cubic Feet
            case "119" -> throw new UnsupportedUnitException("Cubic Feet");
            // Cubic Feet Compensated
            case "120" -> throw new UnsupportedUnitException("Cubic Feet Compensated");
            // Cubic Feet Compensated per Hour
            case "123" -> throw new UnsupportedUnitException("Cubic Feet Compensated per Hour");
            // gram square metres
            case "78" -> throw new UnsupportedUnitException("gram square metres");
            // gPerG
            case "144" -> throw new UnsupportedUnitException("gPerG");
            // Gray
            case "21" -> throw new UnsupportedUnitException("Gray");
            // Hertz per Hertz
            case "150" -> throw new UnsupportedUnitException("Hertz per Hertz");
            // Charcoal per Second
            case "77" -> throw new UnsupportedUnitException("Charcoal per Second");
            // Imperial Gallon
            case "130" -> throw new UnsupportedUnitException("Imperial Gallon");
            // Imperial Gallon per Hour
            case "131" -> throw new UnsupportedUnitException("Imperial Gallon per Hour");
            // Joules per Kelvin
            case "51" -> throw new UnsupportedUnitException("Joules per Kelvin");
            // Joules per Kg
            case "165" -> throw new UnsupportedUnitException("Joules per Kg");
            // Kelvin
            case "6" -> new MeasurementScale(UnitOfMeasureTypeList.K_KELVIN, scale);
            // Katal
            case "158" -> throw new UnsupportedUnitException("Katal");
            // Kg Metres
            case "47" -> throw new UnsupportedUnitException("Kg Metres");
            // Kg per Cubic Metres
            case "48" -> throw new UnsupportedUnitException("Kg Cubic Metres");
            // Litre
            case "134" -> throw new UnsupportedUnitException("Litre");
            // Litre Compensated
            case "157" -> throw new UnsupportedUnitException("Litre Compensated");
            // Litre Compensated per Hour
            case "138" -> throw new UnsupportedUnitException("Litre Compensated per Hour");
            // Litre per Hour
            case "137" -> throw new UnsupportedUnitException("Litre per Hour");
            // Litre per Litre
            case "143" -> throw new UnsupportedUnitException("Litre per Litre");
            // Litre per Second
            case "82" -> throw new UnsupportedUnitException("Litre per Second");
            // Litre Uncompensated
            case "156" -> throw new UnsupportedUnitException("Litre Uncompensated");
            // Litre Uncompensated Per Hour
            case "139" -> throw new UnsupportedUnitException("Litre Uncompensated Per Hour");
            // Lumens
            case "35" -> throw new UnsupportedUnitException("Lumens");
            // Lux
            case "34" -> throw new UnsupportedUnitException("Lux");
            // Square Metres Per Second
            case "49" -> throw new UnsupportedUnitException("Square metres Per Second");
            // Cubic Metres Compensated
            case "167" -> throw new UnsupportedUnitException("Cubic Metres Compensated");
            // Cubic Metres Compensated Per Hour
            case "126" -> throw new UnsupportedUnitException("Cubic Metres Compensated per Hour");
            // Cubic Metres Per Hour
            case "125" -> throw new UnsupportedUnitException("Cubic Metres per Hour");
            // Cubic Metres Per Second
            case "45" -> new MeasurementScale(UnitOfMeasureTypeList.CUBIC_METRES_PER_SECOND, scale);
            // Cubic Metres Compensated
            case "166" -> throw new UnsupportedUnitException("Cubic Metres Compensated");
            // Cubic Metres Compensated Per Hour
            case "127" -> throw new UnsupportedUnitException("Cubic Metres Compensated per Hour");
            // MeCode
            case "118" -> throw new UnsupportedUnitException("meCode");
            // Mole
            case "7" -> throw new UnsupportedUnitException("Mole");
            // Mole per Kg
            case "147" -> throw new UnsupportedUnitException("Mole per Kilogram");
            // Mole per Cubic Metre
            case "145" -> throw new UnsupportedUnitException("Mole per Cubic Metre");
            // Mole per Mole
            case "146" -> throw new UnsupportedUnitException("Mole per Mole");
            // Money
            case "80" -> throw new UnsupportedUnitException("Money");
            // Metres per Metre
            case "148" -> throw new UnsupportedUnitException("Metres per Metre");
            // Metres per Cubic Metre
            case "46" -> throw new UnsupportedUnitException("Metres per Cubic Metre");
            // Metres per Second
            case "43" -> throw new UnsupportedUnitException("Metres per Second");
            // Metres per Second Squared
            case "44" -> throw new UnsupportedUnitException("Metres per Second Squared");
            // Ohm Metres
            case "102" -> throw new UnsupportedUnitException("Ohm Metres");
            // paA
            case "155" -> throw new UnsupportedUnitException("paA");
            // paG
            case "140" -> throw new UnsupportedUnitException("paG");
            // Pounds per Square Inch Absolute
            case "141" -> throw new UnsupportedUnitException("Pounds per Square Inch Absolute");
            // Pounds per Square Inch Gauge
            case "142" -> throw new UnsupportedUnitException("Pounds per Square Inch Gauge");
            // Quantity Power
            case "100" -> throw new UnsupportedUnitException("Quantity Power");
            // q45
            case "161" -> throw new UnsupportedUnitException("Quantity Power 45");
            // q45h
            case "163" -> throw new UnsupportedUnitException("Quantity Energy 45");
            // q60
            case "162" -> throw new UnsupportedUnitException("Quantity Power 60");
            // q60h
            case "164" -> throw new UnsupportedUnitException("Quantity Energy 60");
            // Quantity Energy
            case "101" -> throw new UnsupportedUnitException("Quantity Energy");
            // Radians per Second
            case "54" -> throw new UnsupportedUnitException("Radians per Second");
            // Revolutions
            case "154" -> throw new UnsupportedUnitException("Revolutions");
            // Revolutions per Second
            case "4" -> throw new UnsupportedUnitException("Revolutions per Second");
            // Seconds per Second
            case "149" -> throw new UnsupportedUnitException("Seconds per Second");
            // Steradians
            case "11" -> throw new UnsupportedUnitException("Steradians");
            // status
            case "109" -> throw new UnsupportedUnitException("Status");
            // Sieverts
            case "24" -> throw new UnsupportedUnitException("Sieverts");
            // Tesla
            case "37" -> throw new UnsupportedUnitException("Tesla");
            // Therm
            case "169" -> throw new UnsupportedUnitException("Therm");
            // timeStamp
            case "108" -> throw new UnsupportedUnitException("timeStamp");
            // Gallon
            case "128" -> throw new UnsupportedUnitException("Gallon");
            // Gallons per Hour
            case "129" -> throw new UnsupportedUnitException("Gallons per Hour");
            // Volt Squared
            case "67" -> throw new UnsupportedUnitException("Volt Squared");
            // Volt Squared Hours
            case "104" -> throw new UnsupportedUnitException("Volt Squared Hours");
            // Volt Ampere Hours per Revolution
            case "117" -> throw new UnsupportedUnitException("Volt Ampere Hours per Revolution");
            // Volt Ampere Reactive Hours per Revolution
            case "116" -> throw new UnsupportedUnitException("Volt Ampere Reactive Hours per Revolution");
            // Volt per Hertz
            case "74" -> throw new UnsupportedUnitException("Volt per Hertz");
            // Volt per Volt
            case "151" -> throw new UnsupportedUnitException("Volt per Volt");
            // Volt Seconds
            case "66" -> throw new UnsupportedUnitException("Volt Seconds");
            // Weber
            case "36" -> throw new UnsupportedUnitException("Weber");
            // Watt Hour per Cubic Metre
            case "107" -> throw new UnsupportedUnitException("Watt Hours per Cubic Metre");
            // Watt Hour per Revolution
            case "115" -> throw new UnsupportedUnitException("Watt Hours per Revolution");
            // Watt per Metres Kelvin
            case "50" -> throw new UnsupportedUnitException("Watts per Metres Kelvin");
            // Watts per Second
            case "81" -> throw new UnsupportedUnitException("Watts per Second");
            // Watts per Volt Ampere
            case "153" -> throw new UnsupportedUnitException("Watts per Volt Ampere");
            // Watts per Watt
            case "168" -> throw new UnsupportedUnitException("Watts per Watt");
            default -> throw new UnsupportedUnitException(uom);
        };
    }

    private int calculateScale(int addScale) {
        return addScale + scale;
    }
}
