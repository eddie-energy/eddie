package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModbusVendorTest {

    @Test
    void constructorShouldParseStringIdToUUID() {
        String uuidStr = "123e4567-e89b-12d3-a456-426614174000";
        String name = "Test Vendor";
        ModbusVendor vendor = new ModbusVendor(uuidStr, name);

        assertEquals(UUID.fromString(uuidStr), vendor.id());
        assertEquals(name, vendor.name());
    }

    @Test
    void gettersShouldReturnCorrectValues() {
        UUID id = UUID.randomUUID();
        String name = "Vendor A";
        ModbusVendor vendor = new ModbusVendor(id.toString(), name);

        assertEquals(id, vendor.id());
        assertEquals(name, vendor.name());
    }
}
