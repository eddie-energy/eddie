package energy.eddie.aiida.web;

import energy.eddie.aiida.models.modbus.Device;
import energy.eddie.aiida.models.modbus.Model;
import energy.eddie.aiida.models.modbus.Vendor;
import energy.eddie.aiida.services.ModbusDeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
        List<Vendor> vendors = List.of(new Vendor(UUID.randomUUID().toString(), "TestVendor"));
        when(mockService.getVendors()).thenReturn(vendors);

        ResponseEntity<List<Vendor>> response = controller.getAllModbusVendors();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(vendors, response.getBody());
    }

    @Test
    void getAllModbusVendors_shouldThrowIfEmpty() {
        when(mockService.getVendors()).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, controller::getAllModbusVendors);
    }

    @Test
    void getModelsByVendor_shouldReturnList() {
        String vendorId = UUID.randomUUID().toString();
        List<Model> models = List.of(new Model(UUID.randomUUID().toString(), "Model A", vendorId));
        when(mockService.getModels(vendorId)).thenReturn(models);

        ResponseEntity<List<Model>> response = controller.getModelsByVendor(vendorId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(models, response.getBody());
    }

    @Test
    void getModelsByVendor_shouldThrowIfEmpty() {
        when(mockService.getModels(anyString())).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> controller.getModelsByVendor("invalid-vendor-id"));
    }

    @Test
    void getDevicesByModel_shouldReturnList() {
        String modelId = UUID.randomUUID().toString();
        List<Device> devices = List.of(new Device(UUID.randomUUID().toString(), "Device A", modelId));
        when(mockService.getDevices(modelId)).thenReturn(devices);

        ResponseEntity<List<Device>> response = controller.getDevicesByModel(modelId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(devices, response.getBody());
    }

    @Test
    void getDevicesByModel_shouldThrowIfEmpty() {
        when(mockService.getDevices(anyString())).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> controller.getDevicesByModel("invalid-model-id"));
    }
}
