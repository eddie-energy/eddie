package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModbusModelTest {

    @Test
    void constructorAndGettersShouldWork() {
        String idStr = "123e4567-e89b-12d3-a456-426614174000";
        String vendorIdStr = "987e6543-e21b-12d3-a456-426655440000";
        String name = "Test Model";

        ModbusModel modbusModel = new ModbusModel(idStr, name, vendorIdStr);

        assertEquals(UUID.fromString(idStr), modbusModel.id());
        assertEquals(name, modbusModel.name());
        assertEquals(UUID.fromString(vendorIdStr), modbusModel.vendorId());
    }
}
