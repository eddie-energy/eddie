package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import energy.eddie.aiida.errors.datasource.modbus.ModbusDeviceConfigException;
import energy.eddie.aiida.models.modbus.*;
import jakarta.annotation.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ModbusDeviceService {

    private final List<ModbusVendor> vendors;
    private final List<ModbusModel> modbusModels;
    private final List<Device> devices;

    public ModbusDeviceService() {
        this.vendors = List.of(
                new ModbusVendor("cbdfbd39-24c9-469f-bc7f-ef73b9834ec1", "Carlo Gavazzi"),
                new ModbusVendor("07e0b9ae-8b30-4cf9-8c3c-32de49e3aa56", "Oesterreichs Energie")
        );

        this.modbusModels = List.of(
                new ModbusModel("9875b409-2040-4a2e-b8df-80c3e81bd3d7",
                                "Carlo Gavazzi EM24",
                                String.valueOf(vendors.getFirst().id())),
                new ModbusModel("91d8b15b-bb88-47d3-8425-15cf997bd1d9",
                                "Oesterreichs Energie Adapter",
                                String.valueOf(vendors.get(1).id()))
        );

        this.devices = List.of(
                new Device("26f5dbb2-d1a3-42cb-93d0-5e71ac62e5fc",
                           "Carlo Gavazzi EM24 Default",
                           String.valueOf(modbusModels.getFirst().id())),
                new Device("cfd870cd-fc1d-4288-bba5-414ceaf6e2d7",
                           "Oesterreichs Energie Adapter",
                           String.valueOf(modbusModels.get(1).id()))
        );
    }

    ModbusDeviceService(List<ModbusVendor> vendors, List<ModbusModel> modbusModels, List<Device> devices) {
        this.vendors = vendors;
        this.modbusModels = modbusModels;
        this.devices = devices;
    }

    public List<ModbusVendor> vendors() {
        return this.vendors;
    }

    public List<ModbusModel> models(UUID vendorId) {
        return modbusModels.stream()
                           .filter(m -> m.vendorId().equals(vendorId))
                           .toList();
    }

    public List<ModbusModel> models(String vendorId) {
        return models(UUID.fromString(vendorId));
    }

    public List<Device> devices(UUID modelId) {
        return devices.stream()
                      .filter(m -> m.modelId().equals(modelId))
                      .toList();
    }

    public List<Device> devices(String modelId) {
        return devices(UUID.fromString(modelId));
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

            if (configWrapper == null || configWrapper.devices() == null || configWrapper.devices().isEmpty()) {
                throw new IllegalStateException("Failed to load device config for " + deviceId);
            }

            return configWrapper.devices().getFirst();
        } catch (IOException e) {
            throw new ModbusDeviceConfigException("Failed to load device config for " + deviceId, e);
        }
    }
}
