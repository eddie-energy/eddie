package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @Test
    void testConstructorAndGetters() {
        String idStr = UUID.randomUUID().toString();
        String name = "Test Device";
        String modelIdStr = UUID.randomUUID().toString();

        Device device = new Device(idStr, name, modelIdStr);

        assertEquals(UUID.fromString(idStr), device.getId());
        assertEquals(name, device.getName());
        assertEquals(UUID.fromString(modelIdStr), device.getModelId());
    }

    @Test
    void testSetId() {
        UUID initialId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        Device device = new Device(initialId.toString(), "Device", UUID.randomUUID().toString());

        device.setId(newId);
        assertEquals(newId, device.getId());
    }

    @Test
    void testSetName() {
        Device device = new Device(UUID.randomUUID().toString(), "Old Name", UUID.randomUUID().toString());
        String newName = "New Device Name";

        device.setName(newName);
        assertEquals(newName, device.getName());
    }
}
