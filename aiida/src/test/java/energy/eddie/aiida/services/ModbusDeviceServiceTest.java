// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.datasource.modbus.ModbusDeviceConfigException;
import energy.eddie.aiida.models.modbus.ModbusDevice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModbusDeviceService.class)
class ModbusDeviceServiceTest {

    @Autowired
    private ModbusDeviceService service;

    @Test
    void testCreatesServiceWithBuiltInVendors() {
        var vendors = service.vendors();
        assertNotNull(vendors);
        assertFalse(vendors.isEmpty());
        assertEquals("Carlo Gavazzi", vendors.getFirst().name());
    }

    @Test
    void testModelsByVendorId() {
        UUID vendorId = UUID.fromString("cbdfbd39-24c9-469f-bc7f-ef73b9834ec1"); // Carlo Gavazzi
        var models = service.models(vendorId);
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertEquals("Carlo Gavazzi EM24", models.getFirst().name());
    }

    @Test
    void testModelsByVendorIdString() {
        String vendorId = "cbdfbd39-24c9-469f-bc7f-ef73b9834ec1"; // Carlo Gavazzi
        var models = service.models(vendorId);
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertEquals("Carlo Gavazzi EM24", models.getFirst().name());
    }

    @Test
    void testDevicesByModelId() {
        UUID modelId = UUID.fromString("9875b409-2040-4a2e-b8df-80c3e81bd3d7"); // Carlo Gavazzi EM24
        var devices = service.devices(modelId);
        assertNotNull(devices);
        assertFalse(devices.isEmpty());
        assertEquals("Carlo Gavazzi EM24 Default", devices.getFirst().name());
    }

    @Test
    void testDevicesByModelIdString() {
        String modelId = "9875b409-2040-4a2e-b8df-80c3e81bd3d7"; // Carlo Gavazzi EM24
        var devices = service.devices(modelId);
        assertNotNull(devices);
        assertEquals(1, devices.size());
    }

    @Test
    void testLoadConfigSuccess() {
        UUID deviceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ModbusDevice device = ModbusDeviceService.loadConfig(deviceId);

        assertNotNull(device);
        assertEquals("11111111-1111-1111-1111-111111111111", device.id());
        assertEquals("Test Device-1", device.name());
        assertNotNull(device.sources());
        assertEquals(1, device.sources().getFirst().dataPoints().size());
    }


    @Test
    void testLoadConfigNullIdThrows() {
        assertThrows(ModbusDeviceConfigException.class, () -> ModbusDeviceService.loadConfig(null));
    }

    @Test
    void testLoadConfigInvalidThrows() {
        UUID deviceId = UUID.randomUUID();
        assertThrows(ModbusDeviceConfigException.class, () -> ModbusDeviceService.loadConfig(deviceId));
    }
}
