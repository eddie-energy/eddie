package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DeviceConfigWrapperTest {

    @Test
    void testGetModbus_WhenNull_ReturnsNull() {
        DeviceConfigWrapper wrapper = new DeviceConfigWrapper();
        assertNull(wrapper.getModbus());
    }

    @Test
    void testSetAndGetModbus() {
        DeviceConfigWrapper wrapper = new DeviceConfigWrapper();
        DeviceConfigWrapper.ModbusWrapper modbusWrapper = new DeviceConfigWrapper.ModbusWrapper();
        wrapper.setModbus(modbusWrapper);
        assertEquals(modbusWrapper, wrapper.getModbus());
    }

    @Test
    void testGetDevices_WhenModbusIsNull_ReturnsEmptyList() {
        DeviceConfigWrapper wrapper = new DeviceConfigWrapper();
        List<ModbusDevice> devices = wrapper.getDevices();
        assertNotNull(devices);
        assertTrue(devices.isEmpty());
    }

    @Test
    void testGetDevices_WhenModbusDevicesIsNull_ReturnsEmptyList() {
        DeviceConfigWrapper.ModbusWrapper modbusWrapper = new DeviceConfigWrapper.ModbusWrapper();
        DeviceConfigWrapper wrapper = new DeviceConfigWrapper();
        wrapper.setModbus(modbusWrapper);

        List<ModbusDevice> devices = wrapper.getDevices();
        assertNotNull(devices);
        assertTrue(devices.isEmpty());
    }

    @Test
    void testGetDevices_WhenModbusHasDevices_ReturnsDevices() {
        ModbusDevice device1 = mock(ModbusDevice.class);
        ModbusDevice device2 = mock(ModbusDevice.class);

        DeviceConfigWrapper.ModbusWrapper modbusWrapper = new DeviceConfigWrapper.ModbusWrapper();
        var deviceList = List.of(device1, device2);

        // Use reflection to set private field (since no setter exists for devices)
        try {
            var field = DeviceConfigWrapper.ModbusWrapper.class.getDeclaredField("devices");
            field.setAccessible(true);
            field.set(modbusWrapper, deviceList);
        } catch (Exception e) {
            fail("Failed to set ModbusWrapper devices via reflection: " + e.getMessage());
        }

        DeviceConfigWrapper wrapper = new DeviceConfigWrapper();
        wrapper.setModbus(modbusWrapper);

        List<ModbusDevice> result = wrapper.getDevices();
        assertEquals(2, result.size());
        assertTrue(result.contains(device1));
        assertTrue(result.contains(device2));
    }
}
