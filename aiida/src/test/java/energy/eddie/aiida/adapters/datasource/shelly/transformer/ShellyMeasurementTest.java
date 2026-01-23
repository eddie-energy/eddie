// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShellyMeasurementTest {
    @Test
    void constructor_setsFieldsForApparentPowerL2_withTotalComponent() {
        var exprectedObisCode = ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2;

        var measurement = new ShellyMeasurement(
                ShellyComponent.EM,
                "b_aprt_power",
                "123.45"
        );
        var aiidaRecordValue = measurement.toAiidaRecordValue();

        assertEquals("em:0:b_aprt_power", aiidaRecordValue.rawTag());
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

        var measurement = new ShellyMeasurement(
                ShellyComponent.EM_DATA,
                "total_act",
                "12345.67"
        );
        var aiidaRecordValue = measurement.toAiidaRecordValue();

        assertEquals("emdata:0:total_act", aiidaRecordValue.rawTag());
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

        var measurement = new ShellyMeasurement(
                ShellyComponent.EM,
                "n_current",
                "1.234"
        );

        var aiidaRecordValue = measurement.toAiidaRecordValue();
        assertEquals("em:0:n_current", aiidaRecordValue.rawTag());
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

        var measurement = new ShellyMeasurement(
                ShellyComponent.EM1_2,
                "act_power",
                "12.34"
        );
        var aiidaRecordValue = measurement.toAiidaRecordValue();

        assertEquals("em1:2:act_power", aiidaRecordValue.rawTag());
        assertEquals(exprectedObisCode, measurement.obisCode());
        assertEquals(exprectedObisCode, aiidaRecordValue.dataTag());
        assertEquals(UnitOfMeasurement.WATT, aiidaRecordValue.rawUnitOfMeasurement());
        assertEquals(UnitOfMeasurement.KILO_WATT, aiidaRecordValue.unitOfMeasurement());
        assertEquals("12.34", aiidaRecordValue.rawValue());
        assertEquals("0.01234", aiidaRecordValue.value());
    }
}
