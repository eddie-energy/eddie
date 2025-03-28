package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import energy.eddie.aiida.exceptions.ModbusDeviceConfigException;
import energy.eddie.aiida.models.modbus.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ModbusDeviceService {
    // Vendor UUIDs
    private static final String VENDOR_SIMULATION = "e440249c-9d44-4a9a-9a19-e77e4174a9ff";
    private static final String VENDOR_CARLO_GAVAZZI = "cbdfbd39-24c9-469f-bc7f-ef73b9834ec1"; // NEW

    // Model UUIDs
    private static final String MODEL_SIMULATION_DEFAULT = "15b77548-8a5e-41ab-a48a-93e024da6ef0";
    private static final String MODEL_CARLO_GAVAZZI_EM24 = "9875b409-2040-4a2e-b8df-80c3e81bd3d7"; // NEW

    // Device UUIDs
    private static final String DEVICE_FRONIUS_SYMO_A = "b69031ea-8b95-4323-a09e-2348cbf460d2";
    private static final String DEVICE_FRONIUS_SYMO_B = "b69031ea-8b95-4323-a09e-2348cbf460d2";
    private static final String DEVICE_CARLO_GAVAZZI_EM24_A = "26f5dbb2-d1a3-42cb-93d0-5e71ac62e5fc"; // NEW

    private final List<Vendor> vendors = List.of(
            new Vendor(VENDOR_SIMULATION, "Simulation"),
            new Vendor(VENDOR_CARLO_GAVAZZI, "Carlo Gavazzi") // NEW
    );

    private final List<Model> models = List.of(
            new Model(MODEL_SIMULATION_DEFAULT, "Simulation", VENDOR_SIMULATION),
            new Model(MODEL_CARLO_GAVAZZI_EM24, "Carlo Gavazzi EM24", VENDOR_CARLO_GAVAZZI) // NEW
    );

    private final List<Device> devices = List.of(
            new Device(DEVICE_FRONIUS_SYMO_A, "Simulation Device A", MODEL_SIMULATION_DEFAULT),
            new Device(DEVICE_FRONIUS_SYMO_B, "Simulation Device B", MODEL_SIMULATION_DEFAULT),
            new Device(DEVICE_CARLO_GAVAZZI_EM24_A, "Carlo Gavazzi EM24 Default", MODEL_CARLO_GAVAZZI_EM24) // NEW
    );





    public List<Vendor> getVendors() {
        return this.vendors;
    }

    public List<Model> getModels(UUID vendorId) {
        return models.stream()
                .filter(m -> m.getVendorId().equals(vendorId))
                .toList();
    }

    public List<Model> getModels(String vendorId) {
        return getModels(UUID.fromString(vendorId));
    }

    public List<Device> getDevices(UUID modelId) {
        return devices.stream()
                .filter(m -> m.getModelId().equals(modelId))
                .toList();
    }

    public List<Device> getDevices(String modelId) {
        return getDevices(UUID.fromString(modelId));
    }

    public static ModbusDevice loadConfig(@Nullable UUID deviceId) {
        if (deviceId == null) {
            throw new ModbusDeviceConfigException("Device UUID must not be null");
        }
        try {
            String filename = "modbus-configs/" + deviceId + ".yml";
            ClassPathResource resource = new ClassPathResource(filename);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            DeviceConfigWrapper configWrapper = mapper.readValue(resource.getInputStream(), DeviceConfigWrapper.class);

            if (configWrapper == null || configWrapper.getDevices() == null || configWrapper.getDevices().isEmpty()) {
                throw new IllegalStateException("Failed to load device config for " + deviceId);
            }

            return configWrapper.getDevices().getFirst();
        } catch (IOException e) {
            throw new ModbusDeviceConfigException("Failed to load device config for " + deviceId, e);
        }
    }

}
