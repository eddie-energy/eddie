// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysTopic;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SmartGatewaysAdapterEnergyTotalsTest {
    @Test
    void cumulativeTotalsRemainTheSameWhenTheActiveTariffChanges() {
        for (String tariff : List.of("0001", "0002")) {
            var batch = meterRegisters();
            batch.put(SmartGatewaysTopic.ELECTRICITY_TARIFF, tariff);

            var message = SmartGatewaysAdapterValueDeserializer.deserialize(batch);

            assertThat(message.electricityDelivered()).isEqualTo(new SmartGatewaysAdapterMessageField(
                    "electricityDelivered", "300.375", UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.POSITIVE_ACTIVE_ENERGY));
            assertThat(message.electricityReturned()).isEqualTo(new SmartGatewaysAdapterMessageField(
                    "electricityReturned", "125.030", UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.NEGATIVE_ACTIVE_ENERGY));
        }
    }

    @Test
    void totalsDoNotRequireAnActiveTariffIndicator() {
        var message = SmartGatewaysAdapterValueDeserializer.deserialize(meterRegisters());

        assertThat(message.electricityDelivered()).isNotNull()
                                                  .extracting(SmartGatewaysAdapterMessageField::value)
                                                  .isEqualTo("300.375");
        assertThat(message.electricityReturned()).isNotNull()
                                                 .extracting(SmartGatewaysAdapterMessageField::value)
                                                 .isEqualTo("125.030");
    }

    @Test
    void missingRegisterDoesNotBecomeAZeroOrAPartialTotal() {
        var batch = meterRegisters();
        batch.remove(SmartGatewaysTopic.ELECTRICITY_DELIVERED_2);
        var message = SmartGatewaysAdapterValueDeserializer.deserialize(batch);

        assertThat(message.electricityDelivered()).isNull();
        assertThat(message.electricityReturned()).isNotNull()
                                                 .extracting(SmartGatewaysAdapterMessageField::value)
                                                 .isEqualTo("125.030");

        batch.remove(SmartGatewaysTopic.ELECTRICITY_RETURNED_1);
        assertThat(SmartGatewaysAdapterValueDeserializer.deserialize(batch).electricityReturned()).isNull();
    }

    @Test
    void emptyBatchDoesNotProduceEnergyTotals() {
        var message = SmartGatewaysAdapterValueDeserializer.deserialize(Map.of());

        assertThat(message.electricityDelivered()).isNull();
        assertThat(message.electricityReturned()).isNull();
    }

    @Test
    void totalsPreserveDecimalPrecisionForLargeMeterReadings() {
        var batch = meterRegisters();
        batch.put(SmartGatewaysTopic.ELECTRICITY_DELIVERED_1, "999999999999.999");
        batch.put(SmartGatewaysTopic.ELECTRICITY_DELIVERED_2, "0.002");

        assertThat(SmartGatewaysAdapterValueDeserializer.deserialize(batch).electricityDelivered())
                .isNotNull()
                .extracting(SmartGatewaysAdapterMessageField::value)
                .isEqualTo("1000000000000.001");
    }

    private static Map<SmartGatewaysTopic, String> meterRegisters() {
        Map<SmartGatewaysTopic, String> batch = new EnumMap<>(SmartGatewaysTopic.class);
        batch.put(SmartGatewaysTopic.ELECTRICITY_DELIVERED_1, "100.125");
        batch.put(SmartGatewaysTopic.ELECTRICITY_DELIVERED_2, "200.250");
        batch.put(SmartGatewaysTopic.ELECTRICITY_RETURNED_1, "50.010");
        batch.put(SmartGatewaysTopic.ELECTRICITY_RETURNED_2, "75.020");
        return batch;
    }
}
