package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @Test
    void testGetters() {
        UUID id = UUID.randomUUID();
        String name = "MyDevice";
        UUID modelId = UUID.randomUUID();

        Device device = new Device(id.toString(), name, modelId.toString());

        assertEquals(id, device.getId(), "getId() should return correct UUID");
        assertEquals(name, device.getName(), "getName() should return correct name");
        assertEquals(modelId, device.getModelId(), "getModelId() should return correct modelId");
    }

    @Test
    void testSetters() {
        Device device = new Device(UUID.randomUUID().toString(), "Old Name", UUID.randomUUID().toString());

        UUID newId = UUID.randomUUID();
        String newName = "New Device Name";

        device.setId(newId);
        device.setName(newName);

        assertEquals(newId, device.getId(), "setId() should update the id");
        assertEquals(newName, device.getName(), "setName() should update the name");
    }
}
