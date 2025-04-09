package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VendorTest {

    @Test
    void constructorShouldParseStringIdToUUID() {
        String uuidStr = "123e4567-e89b-12d3-a456-426614174000";
        String name = "Test Vendor";
        Vendor vendor = new Vendor(uuidStr, name);

        assertEquals(UUID.fromString(uuidStr), vendor.getId());
        assertEquals(name, vendor.getName());
    }

    @Test
    void settersShouldUpdateValues() {
        Vendor vendor = new Vendor("123e4567-e89b-12d3-a456-426614174000", "Initial Name");

        UUID newId = UUID.randomUUID();
        String newName = "Updated Vendor";

        vendor.setId(newId);
        vendor.setName(newName);

        assertEquals(newId, vendor.getId());
        assertEquals(newName, vendor.getName());
    }

    @Test
    void gettersShouldReturnCorrectValues() {
        UUID id = UUID.randomUUID();
        String name = "Vendor A";
        Vendor vendor = new Vendor(id.toString(), name);

        assertEquals(id, vendor.getId());
        assertEquals(name, vendor.getName());
    }
}
