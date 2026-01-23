// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.models.modbus.Device;
import energy.eddie.aiida.models.modbus.ModbusModel;
import energy.eddie.aiida.models.modbus.ModbusVendor;
import energy.eddie.aiida.services.ModbusDeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ModbusDeviceControllerTest {

    private ModbusDeviceService mockService;
    private ModbusDeviceController controller;

    @BeforeEach
    void setUp() {
        mockService = mock(ModbusDeviceService.class);
        controller = new ModbusDeviceController(mockService);
    }

    @Test
    void getAllModbusVendors_shouldReturnList() {
        List<ModbusVendor> vendors = List.of(new ModbusVendor(UUID.randomUUID().toString(), "TestVendor"));
        when(mockService.vendors()).thenReturn(vendors);

        ResponseEntity<List<ModbusVendor>> response = controller.getAllModbusVendors();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(vendors, response.getBody());
    }

    @Test
    void getAllModbusVendors_shouldThrowIfEmpty() {
        when(mockService.vendors()).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, controller::getAllModbusVendors);
    }

    @Test
    void getModelsByVendor_shouldReturnList() {
        String vendorId = UUID.randomUUID().toString();
        List<ModbusModel> modbusModels = List.of(new ModbusModel(UUID.randomUUID().toString(), "Model A", vendorId));
        when(mockService.models(vendorId)).thenReturn(modbusModels);

        ResponseEntity<List<ModbusModel>> response = controller.getModelsByVendor(vendorId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(modbusModels, response.getBody());
    }

    @Test
    void getModelsByVendor_shouldThrowIfEmpty() {
        when(mockService.models(anyString())).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> controller.getModelsByVendor("invalid-vendor-id"));
    }

    @Test
    void getDevicesByModel_shouldReturnList() {
        String modelId = UUID.randomUUID().toString();
        List<Device> devices = List.of(new Device(UUID.randomUUID().toString(), "Device A", modelId));
        when(mockService.devices(modelId)).thenReturn(devices);

        ResponseEntity<List<Device>> response = controller.getDevicesByModel(modelId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(devices, response.getBody());
    }

    @Test
    void getDevicesByModel_shouldThrowIfEmpty() {
        when(mockService.devices(anyString())).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> controller.getDevicesByModel("invalid-model-id"));
    }
}
