// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceTest {

    @Test
    void testGetters() {
        UUID id = UUID.randomUUID();
        String name = "MyDevice";
        UUID modelId = UUID.randomUUID();

        Device device = new Device(id.toString(), name, modelId.toString());

        assertEquals(id, device.id(), "id() should return correct UUID");
        assertEquals(name, device.name(), "name() should return correct name");
        assertEquals(modelId, device.modelId(), "modelId() should return correct modelId");
    }
}
