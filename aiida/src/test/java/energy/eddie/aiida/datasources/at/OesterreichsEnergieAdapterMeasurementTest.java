package energy.eddie.aiida.datasources.at;

import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OesterreichsEnergieAdapterMeasurementTest {
    @Test
    void testOesterreichsEnergieAdapterMeasurement_convertsToRightUnitsAndValues() {
        // Given
        var positiveActiveInstantaneousPower = ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
        var positiveActiveEnergy = ObisCode.POSITIVE_ACTIVE_ENERGY;
        var positiveReactiveInstantaneousPower = ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER;
        var positiveReactiveEnergyInTariff = ObisCode.POSITIVE_REACTIVE_ENERGY_IN_TARIFF;

        var dummyValue = 1000;

        // When
        var oeaMeasurement1 = new OesterreichsEnergieAdapterMeasurement(positiveActiveInstantaneousPower,
                                                                        String.valueOf(dummyValue));
        var oeaMeasurement2 = new OesterreichsEnergieAdapterMeasurement(positiveActiveEnergy,
                                                                        String.valueOf(dummyValue));
        var oeaMeasurement3 = new OesterreichsEnergieAdapterMeasurement(positiveReactiveInstantaneousPower,
                                                                        String.valueOf(dummyValue));
        var oeaMeasurement4 = new OesterreichsEnergieAdapterMeasurement(positiveReactiveEnergyInTariff,
                                                                        String.valueOf(dummyValue));

        var aiidaRecordValue1 = new AiidaRecordValue(
                positiveActiveInstantaneousPower.toString(),
                positiveActiveInstantaneousPower,
                oeaMeasurement1.rawValue(),
                oeaMeasurement1.rawUnitOfMeasurement(),
                oeaMeasurement1.value(),
                oeaMeasurement1.unitOfMeasurement()
        );

        var aiidaRecordValue2 = new AiidaRecordValue(
                positiveActiveEnergy.toString(),
                positiveActiveEnergy,
                oeaMeasurement2.rawValue(),
                oeaMeasurement2.rawUnitOfMeasurement(),
                oeaMeasurement2.value(),
                oeaMeasurement2.unitOfMeasurement()
        );

        var aiidaRecordValue3 = new AiidaRecordValue(
                positiveReactiveInstantaneousPower.toString(),
                positiveReactiveInstantaneousPower,
                oeaMeasurement3.rawValue(),
                oeaMeasurement3.rawUnitOfMeasurement(),
                oeaMeasurement3.value(),
                oeaMeasurement3.unitOfMeasurement()
        );

        var aiidaRecordValue4 = new AiidaRecordValue(
                positiveReactiveEnergyInTariff.toString(),
                positiveReactiveEnergyInTariff,
                oeaMeasurement4.rawValue(),
                oeaMeasurement4.rawUnitOfMeasurement(),
                oeaMeasurement4.value(),
                oeaMeasurement4.unitOfMeasurement()
        );

        // Then
        var expectedValue = String.valueOf(dummyValue / 1000f);

        assertEquals(UnitOfMeasurement.W, aiidaRecordValue1.rawUnitOfMeasurement());
        assertEquals(expectedValue, aiidaRecordValue1.value());
        assertEquals(UnitOfMeasurement.KW, aiidaRecordValue1.unitOfMeasurement());

        assertEquals(UnitOfMeasurement.WH, aiidaRecordValue2.rawUnitOfMeasurement());
        assertEquals(expectedValue, aiidaRecordValue2.value());
        assertEquals(UnitOfMeasurement.KWH, aiidaRecordValue2.unitOfMeasurement());

        assertEquals(UnitOfMeasurement.VAR, aiidaRecordValue3.rawUnitOfMeasurement());
        assertEquals(expectedValue, aiidaRecordValue3.value());
        assertEquals(UnitOfMeasurement.KVAR, aiidaRecordValue3.unitOfMeasurement());

        assertEquals(UnitOfMeasurement.VARH, aiidaRecordValue4.rawUnitOfMeasurement());
        assertEquals(expectedValue, aiidaRecordValue4.value());
        assertEquals(UnitOfMeasurement.KVARH, aiidaRecordValue4.unitOfMeasurement());
    }

    @Test
    void testOesterreichsEnergieAdapterMeasurement_doesNothing() {
        // Given
        var instantaneousPowerFactor = ObisCode.INSTANTANEOUS_POWER_FACTOR;
        var instantaneousCurrentInPhaseL1 = ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1;
        var instantaneousVoltageInPhaseL1 = ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1;
        var unknown = ObisCode.UNKNOWN;

        var dummyValue = "1000";

        // When
        var oeaMeasurement1 = new OesterreichsEnergieAdapterMeasurement(instantaneousPowerFactor, dummyValue);
        var oeaMeasurement2 = new OesterreichsEnergieAdapterMeasurement(instantaneousCurrentInPhaseL1, dummyValue);
        var oeaMeasurement3 = new OesterreichsEnergieAdapterMeasurement(instantaneousVoltageInPhaseL1, dummyValue);
        var oeaMeasurement4 = new OesterreichsEnergieAdapterMeasurement(unknown, dummyValue);

        var aiidaRecordValue1 = new AiidaRecordValue(
                instantaneousPowerFactor.toString(),
                instantaneousPowerFactor,
                oeaMeasurement1.rawValue(),
                oeaMeasurement1.rawUnitOfMeasurement(),
                oeaMeasurement1.value(),
                oeaMeasurement1.unitOfMeasurement()
        );

        var aiidaRecordValue2 = new AiidaRecordValue(
                instantaneousCurrentInPhaseL1.toString(),
                instantaneousCurrentInPhaseL1,
                oeaMeasurement2.rawValue(),
                oeaMeasurement2.rawUnitOfMeasurement(),
                oeaMeasurement2.value(),
                oeaMeasurement2.unitOfMeasurement()
        );

        var aiidaRecordValue3 = new AiidaRecordValue(
                instantaneousVoltageInPhaseL1.toString(),
                instantaneousVoltageInPhaseL1,
                oeaMeasurement3.rawValue(),
                oeaMeasurement3.rawUnitOfMeasurement(),
                oeaMeasurement3.value(),
                oeaMeasurement3.unitOfMeasurement()
        );

        var aiidaRecordValue4 = new AiidaRecordValue(
                unknown.toString(),
                unknown,
                oeaMeasurement4.rawValue(),
                oeaMeasurement4.rawUnitOfMeasurement(),
                oeaMeasurement4.value(),
                oeaMeasurement4.unitOfMeasurement()
        );

        // Then
        assertEquals(UnitOfMeasurement.NONE, aiidaRecordValue1.rawUnitOfMeasurement());
        assertEquals(dummyValue, aiidaRecordValue1.value());
        assertEquals(UnitOfMeasurement.NONE, aiidaRecordValue1.unitOfMeasurement());

        assertEquals(UnitOfMeasurement.AMPERE, aiidaRecordValue2.rawUnitOfMeasurement());
        assertEquals(dummyValue, aiidaRecordValue2.value());
        assertEquals(UnitOfMeasurement.AMPERE, aiidaRecordValue2.unitOfMeasurement());

        assertEquals(UnitOfMeasurement.VOLT, aiidaRecordValue3.rawUnitOfMeasurement());
        assertEquals(dummyValue, aiidaRecordValue3.value());
        assertEquals(UnitOfMeasurement.VOLT, aiidaRecordValue3.unitOfMeasurement());

        assertEquals(UnitOfMeasurement.UNKNOWN, aiidaRecordValue4.rawUnitOfMeasurement());
        assertEquals(dummyValue, aiidaRecordValue4.value());
        assertEquals(UnitOfMeasurement.UNKNOWN, aiidaRecordValue4.unitOfMeasurement());
    }
}
