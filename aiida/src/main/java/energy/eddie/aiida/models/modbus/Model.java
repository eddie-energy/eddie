package energy.eddie.aiida.models.modbus;

import java.util.UUID;

public class Model {
    private UUID id;
    private String name;
    private final UUID vendorId;

    public Model(String id, String name, String vendorId) {
        this.id = UUID.fromString(id);
        this.name = name;
        this.vendorId = UUID.fromString(vendorId);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getVendorId() {
        return vendorId;
    }
}

