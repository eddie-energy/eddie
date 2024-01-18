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

    public MasterDataService(ObjectMapper objectMapper) throws FileNotFoundException {
        try {
            this.permissionAdministrators = objectMapper.readValue(
                    getClass().getClassLoader().getResource("permission-administrators.json"),
                    new TypeReference<>() {
                    }
            );
        } catch (IOException e) {
            throw new FileNotFoundException("Error while reading permission administrators from config file");
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
}