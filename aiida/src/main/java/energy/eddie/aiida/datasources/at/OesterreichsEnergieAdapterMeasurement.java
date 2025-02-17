package energy.eddie.aiida.datasources.at;

import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

public class OesterreichsEnergieAdapterMeasurement {
    private final ObisCode obisCode;
    private final String rawValue;

    public OesterreichsEnergieAdapterMeasurement(ObisCode obisCode, String rawValue) {
        this.obisCode = obisCode;
        this.rawValue = rawValue;
    }

    public UnitOfMeasurement unitOfMeasurement() {
        return obisCode.unitOfMeasurement();
    }

    public String rawValue() {
        return rawValue;
    }

    /**
     * <p>
     * Normally OBIS code values are prefixed with a factor 1000 (kilo) like stated by multiple sources such as <a href="https://www.promotic.eu/en/pmdoc/Subsystems/Comm/PmDrivers/PmIEC62056/IEC62056_OBIS.htm">Promotic</a> and <a href="https://onemeter.com/docs/device/obis/">OneMeter</a>.
     * See <a href="https://oesterreichsenergie.at/fileadmin/user_upload/Smart_Meter-Plattform/20200201_Konzept_Kundenschnittstelle_SM.pdf">Concept</a> of the OesterreichsEnergieAdapter to view the transmitted values and their units.
     * </p>
     *
     * @return the raw unit of the measurement.
     */
    public UnitOfMeasurement rawUnitOfMeasurement() {
        return switch (obisCode.unitOfMeasurement()) {
            case W, KW -> UnitOfMeasurement.W;
            case WH, KWH -> UnitOfMeasurement.WH;
            case VAR, KVAR -> UnitOfMeasurement.VAR;
            case VARH, KVARH -> UnitOfMeasurement.VARH;
            case AMPERE -> UnitOfMeasurement.AMPERE;
            case VOLT -> UnitOfMeasurement.VOLT;
            case VOLTAMPERE -> UnitOfMeasurement.VOLTAMPERE;
            case NONE -> UnitOfMeasurement.NONE;
            case UNKNOWN -> UnitOfMeasurement.UNKNOWN;
        };
    }

    /**
     * <p>
     * Adapts the raw value to the correct unit of measurement.
     * For OesterreichsEnergieAdapter this means that values with a unit of measurement of kilo (k) need to be divided by 1000 as they are transmitted without the factor 1000.
     * </p>
     *
     * @return the value.
     */
    public String value() {
        return switch (obisCode.unitOfMeasurement()) {
            case W, VARH, VAR, WH, AMPERE, VOLTAMPERE, NONE, UNKNOWN, VOLT -> rawValue;
            case KW, KVAR, KWH, KVARH -> String.valueOf(Double.parseDouble(rawValue) / 1000);
        };
    }
}
