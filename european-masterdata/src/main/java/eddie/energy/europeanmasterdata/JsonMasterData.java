package eddie.energy.europeanmasterdata;

import energy.eddie.api.agnostic.master.data.MasterData;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

@Component
public class JsonMasterData implements MasterData {

    private final List<PermissionAdministrator> permissionAdministrators;
    private final List<MeteredDataAdministrator> meteredDataAdministrators;

    private final ObjectMapper objectMapper;

    public JsonMasterData(ObjectMapper objectMapper) throws FileNotFoundException {
        this.objectMapper = objectMapper;

        this.permissionAdministrators = readJsonFile("permission-administrators.json",
                                                     PermissionAdministrator.class);
        this.meteredDataAdministrators = readJsonFile("metered-data-administrators.json",
                                                      MeteredDataAdministrator.class);
    }

    private <T> List<T> readJsonFile(String filename, Class<T> elementType) throws FileNotFoundException {
        try {
            return objectMapper.readValue(
                    getClass().getClassLoader().getResourceAsStream(filename),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
            );
        } catch (JacksonException e) {
            throw new FileNotFoundException("Error reading config file " + filename + ": " + e.getMessage());
        }
    }

    @Override
    public List<PermissionAdministrator> permissionAdministrators() {
        return permissionAdministrators;
    }

    @Override
    public Optional<PermissionAdministrator> getPermissionAdministrator(String id) {
        return permissionAdministrators.stream()
                                       .filter(p -> p.companyId().equals(id))
                                       .findFirst();
    }

    @Override
    public List<MeteredDataAdministrator> meteredDataAdministrators() {
        return meteredDataAdministrators;
    }

    @Override
    public Optional<MeteredDataAdministrator> getMeteredDataAdministrator(String id) {
        return meteredDataAdministrators.stream()
                                        .filter(m -> m.companyId().equals(id))
                                        .findFirst();
    }
}
