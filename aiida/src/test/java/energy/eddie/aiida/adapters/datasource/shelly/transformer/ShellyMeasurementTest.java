package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellyMeasurementTest {
    @Test
    void constructor_setsFieldsForApparentPowerL2_withTotalComponent() {
        var exprectedObisCode = ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2;
        var entryKey = "b_aprt_power";

        var measurement = new ShellyMeasurement(
                ShellyComponent.EM,
                entryKey,
                "123.45"
        );
        var aiidaRecordValue = measurement.toAiidaRecordValue();

        assertEquals(entryKey, aiidaRecordValue.rawTag());
        assertEquals(exprectedObisCode, measurement.obisCode());
        assertEquals(exprectedObisCode, aiidaRecordValue.dataTag());
        assertEquals(UnitOfMeasurement.VOLT_AMPERE_REACTIVE, aiidaRecordValue.rawUnitOfMeasurement());
        assertEquals(UnitOfMeasurement.KILO_VOLT_AMPERE_REACTIVE, aiidaRecordValue.unitOfMeasurement());
        assertEquals("123.45", aiidaRecordValue.rawValue());
        assertEquals("0.12345", aiidaRecordValue.value());
    }

    @Test
    void constructor_setsFieldsForActiveEnergyTotal_withTotalComponent() {
        var exprectedObisCode = ObisCode.POSITIVE_ACTIVE_ENERGY;
        var entryKey = "total_act";

        var measurement = new ShellyMeasurement(
                ShellyComponent.EM_DATA,
                entryKey,
                "12345.67"
        );
        var aiidaRecordValue = measurement.toAiidaRecordValue();

        assertEquals(entryKey, aiidaRecordValue.rawTag());
        assertEquals(exprectedObisCode, measurement.obisCode());
        assertEquals(exprectedObisCode, aiidaRecordValue.dataTag());
        assertEquals(UnitOfMeasurement.WATT_HOUR, aiidaRecordValue.rawUnitOfMeasurement());
        assertEquals(UnitOfMeasurement.KILO_WATT_HOUR, aiidaRecordValue.unitOfMeasurement());
        assertEquals("12345.67", aiidaRecordValue.rawValue());
        assertEquals("12.34567", aiidaRecordValue.value());
    }

    @Test
    void constructor_setsFieldsForCurrent_withTotalComponent() {
        var exprectedObisCode = ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_NEUTRAL;
        var entryKey = "n_current";

        var measurement = new ShellyMeasurement(
                ShellyComponent.EM,
                entryKey,
                "1.234"
        );

        var aiidaRecordValue = measurement.toAiidaRecordValue();
        assertEquals(entryKey, aiidaRecordValue.rawTag());
        assertEquals(exprectedObisCode, measurement.obisCode());
        assertEquals(exprectedObisCode, aiidaRecordValue.dataTag());
        assertEquals(UnitOfMeasurement.AMPERE, aiidaRecordValue.rawUnitOfMeasurement());
        assertEquals(UnitOfMeasurement.AMPERE, aiidaRecordValue.unitOfMeasurement());
        assertEquals("1.234", aiidaRecordValue.rawValue());
        assertEquals("1.234", aiidaRecordValue.value());
    }

    @Test
    void constructor_setsFieldsForActivePower_withL3Component() {
        var exprectedObisCode = ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L3;
        var entryKey = "act_power";

        var measurement = new ShellyMeasurement(
                ShellyComponent.EM1_2,
                entryKey,
                "12.34"
        );
        var aiidaRecordValue = measurement.toAiidaRecordValue();

        assertEquals(entryKey, aiidaRecordValue.rawTag());
        assertEquals(exprectedObisCode, measurement.obisCode());
        assertEquals(exprectedObisCode, aiidaRecordValue.dataTag());
        assertEquals(UnitOfMeasurement.WATT, aiidaRecordValue.rawUnitOfMeasurement());
        assertEquals(UnitOfMeasurement.KILO_WATT, aiidaRecordValue.unitOfMeasurement());
        assertEquals("12.34", aiidaRecordValue.rawValue());
        assertEquals("0.01234", aiidaRecordValue.value());
    }
}
