package energy.eddie.aiida.models.modbus;

import java.util.UUID;

public class Vendor {
    private UUID id;
    private String name;

    public Vendor(String id, String name) {
        this.id = UUID.fromString(id);
        this.name = name;
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
}
