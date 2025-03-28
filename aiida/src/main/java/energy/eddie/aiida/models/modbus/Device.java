package energy.eddie.aiida.models.modbus;

import java.util.UUID;

public class Device {
    private UUID id;
    private String name;
    private final UUID modelId;

    public Device(String id, String name, String modelId) {
        this.id = UUID.fromString(id);
        this.name = name;
        this.modelId = UUID.fromString(modelId);
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

    public UUID getModelId() {
        return modelId;
    }
}
