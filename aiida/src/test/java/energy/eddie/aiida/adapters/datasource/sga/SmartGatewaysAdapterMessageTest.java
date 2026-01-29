// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysTopic;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SmartGatewaysAdapterValueDeserializerTest {

    @Test
    void testDeserializeSelectedFields() {
        Map<SmartGatewaysTopic, String> batch = new EnumMap<>(SmartGatewaysTopic.class);

        batch.put(SmartGatewaysTopic.ELECTRICITY_TARIFF, "0002");
        batch.put(SmartGatewaysTopic.ELECTRICITY_DELIVERED_1, "3845.467");
        batch.put(SmartGatewaysTopic.ELECTRICITY_RETURNED_2, "1435.228");
        batch.put(SmartGatewaysTopic.ELECTRICITY_CURRENTLY_DELIVERED, "0.531");
        batch.put(SmartGatewaysTopic.PHASE_CURRENTLY_DELIVERED_L3, "460");

        var result = SmartGatewaysAdapterValueDeserializer.deserialize(batch);

        assertEquals("0002", result.electricityTariff().value());
        assertEquals("3845.467", result.electricityDeliveredTariff1().value());
        assertEquals("1435.228", result.electricityReturnedTariff2().value());
        assertEquals("0.531", result.powerCurrentlyDelivered().value());
        assertEquals("460", result.phaseCurrentlyDeliveredL3().value());
    }

    @Test
    void testEmptyBatchProducesNullFields() {
        Map<SmartGatewaysTopic, String> batch = new EnumMap<>(SmartGatewaysTopic.class);
        var result = SmartGatewaysAdapterValueDeserializer.deserialize(batch);

        assertNull(result.electricityTariff());
        assertNull(result.electricityDeliveredTariff1());
        assertNull(result.electricityReturnedTariff2());
    }
}
