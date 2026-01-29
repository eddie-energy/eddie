// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DeviceConfigWrapperTest {

    @Test
    void testGetModbus_WhenNull_ReturnsNull() {
        DeviceConfigWrapper wrapper = new DeviceConfigWrapper(null);
        assertNull(wrapper.modbus());
    }

    @Test
    void testSetAndGetModbus() {
        DeviceConfigWrapper.ModbusWrapper modbusWrapper = new DeviceConfigWrapper.ModbusWrapper(null);
        DeviceConfigWrapper wrapper = new DeviceConfigWrapper(modbusWrapper);
        assertEquals(modbusWrapper, wrapper.modbus());
    }

    @Test
    void testDevices_WhenModbusIsNull_ReturnsEmptyList() {
        DeviceConfigWrapper wrapper = new DeviceConfigWrapper(null);
        List<ModbusDevice> devices = wrapper.devices();
        assertNotNull(devices);
        assertTrue(devices.isEmpty());
    }

    @Test
    void testDevicesIsNull_ReturnsEmptyList() {
        DeviceConfigWrapper.ModbusWrapper modbusWrapper = new DeviceConfigWrapper.ModbusWrapper(null);
        DeviceConfigWrapper wrapper = new DeviceConfigWrapper(modbusWrapper);

        List<ModbusDevice> devices = wrapper.devices();
        assertNotNull(devices);
        assertTrue(devices.isEmpty());
    }

    @Test
    void testGetDevices_WhenModbusHasDevices_ReturnsDevices() {
        ModbusDevice device1 = mock(ModbusDevice.class);
        ModbusDevice device2 = mock(ModbusDevice.class);

        var deviceList = List.of(device1, device2);
        DeviceConfigWrapper.ModbusWrapper modbusWrapper = new DeviceConfigWrapper.ModbusWrapper(deviceList);
        DeviceConfigWrapper wrapper = new DeviceConfigWrapper(modbusWrapper);

        List<ModbusDevice> result = wrapper.devices();
        assertEquals(2, result.size());
        assertTrue(result.contains(device1));
        assertTrue(result.contains(device2));
    }

}
