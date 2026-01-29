// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.datasource.modbus.ModbusDeviceConfigException;
import energy.eddie.aiida.models.modbus.Device;
import energy.eddie.aiida.models.modbus.ModbusDevice;
import energy.eddie.aiida.models.modbus.ModbusModel;
import energy.eddie.aiida.models.modbus.ModbusVendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModbusDeviceServiceTest {

    private ModbusDeviceService service;

    @BeforeEach
    void setUp() {
        var testVendors = List.of(
                new ModbusVendor("e440249c-9d44-4a9a-9a19-e77e4174a9ff", "Simulation"),
                new ModbusVendor("cbdfbd39-24c9-469f-bc7f-ef73b9834ec1", "Carlo Gavazzi"),
                new ModbusVendor("07e0b9ae-8b30-4cf9-8c3c-32de49e3aa56", "Oesterreichs Energie")
        );

        var testModels = List.of(
                new ModbusModel("15b77548-8a5e-41ab-a48a-93e024da6ef0",
                                "Simulation",
                                String.valueOf(testVendors.get(0).id())),
                new ModbusModel("9875b409-2040-4a2e-b8df-80c3e81bd3d7",
                                "Carlo Gavazzi EM24",
                                String.valueOf(testVendors.get(1).id())),
                new ModbusModel("91d8b15b-bb88-47d3-8425-15cf997bd1d9",
                                "Oesterreichs Energie Adapter",
                                String.valueOf(testVendors.get(2).id()))
        );

        var testDevices = List.of(
                new Device("b69031ea-8b95-4323-a09e-2348cbf460d2",
                           "Simulation Device A",
                           String.valueOf(testModels.get(0).id())),
                new Device("b69031ea-8b95-4323-a09e-2348cbf460d2",
                           "Simulation Device B",
                           String.valueOf(testModels.get(0).id())),
                new Device("26f5dbb2-d1a3-42cb-93d0-5e71ac62e5fc",
                           "Carlo Gavazzi EM24 Default",
                           String.valueOf(testModels.get(1).id())),
                new Device("cfd870cd-fc1d-4288-bba5-414ceaf6e2d7",
                           "Oesterreichs Energie Adapter",
                           String.valueOf(testModels.get(2).id()))
        );

        service = new ModbusDeviceService(testVendors, testModels, testDevices);
    }

    @Test
    void testVendorsNotEmpty() {
        List<ModbusVendor> vendors = service.vendors();
        assertNotNull(vendors);
        assertFalse(vendors.isEmpty());
    }

    @Test
    void testModelsByVendorId() {
        UUID vendorId = UUID.fromString("e440249c-9d44-4a9a-9a19-e77e4174a9ff"); // Simulation
        List<ModbusModel> models = service.models(vendorId);
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertEquals("Simulation", models.getFirst().name());
    }

    @Test
    void testModelsByVendorIdString() {
        String vendorId = "cbdfbd39-24c9-469f-bc7f-ef73b9834ec1"; // Carlo Gavazzi
        List<ModbusModel> models = service.models(vendorId);
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertEquals("Carlo Gavazzi EM24", models.getFirst().name());
    }

    @Test
    void testDevicesByModelId() {
        UUID modelId = UUID.fromString("9875b409-2040-4a2e-b8df-80c3e81bd3d7"); // Carlo Gavazzi EM24
        List<Device> devices = service.devices(modelId);
        assertNotNull(devices);
        assertFalse(devices.isEmpty());
        assertEquals("Carlo Gavazzi EM24 Default", devices.getFirst().name());
    }

    @Test
    void testDevicesByModelIdString() {
        String modelId = "15b77548-8a5e-41ab-a48a-93e024da6ef0"; // Simulation
        List<Device> devices = service.devices(modelId);
        assertNotNull(devices);
        assertEquals(2, devices.size());
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
