package energy.eddie.core.masterdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MasterDataService {

    private final List<PermissionAdministrator> permissionAdministrators;
    private final List<MeteredDataAdministrator> meteredDataAdministrators;

    private final ObjectMapper objectMapper;

    public MasterDataService(ObjectMapper objectMapper) throws FileNotFoundException {
        this.objectMapper = objectMapper;

        this.permissionAdministrators = readJsonFile("permission-administrators.json");
        this.meteredDataAdministrators = readJsonFile("metered-data-administrators.json");
    }

    private <T> List<T> readJsonFile(String filename) throws FileNotFoundException {
        try {
            return objectMapper.readValue(
                    getClass().getClassLoader().getResource(filename),
                    new TypeReference<>() {
                    }
            );
        } catch (IOException e) {
            throw new FileNotFoundException("Error reading config file " + filename + ": " + e.getMessage());
        }
    }

    public List<PermissionAdministrator> getPermissionAdministrators() {
        return permissionAdministrators;
    }

    public Optional<PermissionAdministrator> getPermissionAdministrator(String id) {
        return permissionAdministrators.stream()
                .filter(p -> p.companyId().equals(id))
                .findFirst();
    }

    public List<MeteredDataAdministrator> getMeteredDataAdministrators() {
        return meteredDataAdministrators;
    }

    public Optional<MeteredDataAdministrator> getMeteredDataAdministrator(String id) {
        return meteredDataAdministrators.stream()
                .filter(m -> m.companyId().equals(id))
                .findFirst();
    }
}