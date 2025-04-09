package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void constructorAndGettersShouldWork() {
        String idStr = "123e4567-e89b-12d3-a456-426614174000";
        String vendorIdStr = "987e6543-e21b-12d3-a456-426655440000";
        String name = "Test Model";

        Model model = new Model(idStr, name, vendorIdStr);

        assertEquals(UUID.fromString(idStr), model.getId());
        assertEquals(name, model.getName());
        assertEquals(UUID.fromString(vendorIdStr), model.getVendorId());
    }

    @Test
    void settersShouldModifyState() {
        String idStr = "123e4567-e89b-12d3-a456-426614174000";
        String vendorIdStr = "987e6543-e21b-12d3-a456-426655440000";
        String name = "Initial Name";

        Model model = new Model(idStr, name, vendorIdStr);

        UUID newId = UUID.randomUUID();
        String newName = "Updated Name";

        model.setId(newId);
        model.setName(newName);

        assertEquals(newId, model.getId());
        assertEquals(newName, model.getName());
        // vendorId is final and should remain unchanged
        assertEquals(UUID.fromString(vendorIdStr), model.getVendorId());
    }
}
