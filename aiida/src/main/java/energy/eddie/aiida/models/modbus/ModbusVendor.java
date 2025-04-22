package energy.eddie.aiida.models.modbus;

import java.util.UUID;

public record ModbusVendor(
        UUID id,
        String name
) {
    public ModbusVendor(String id, String name) {
        this(UUID.fromString(id), name);
    }
}
