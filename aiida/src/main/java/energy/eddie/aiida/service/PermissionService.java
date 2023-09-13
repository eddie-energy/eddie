package energy.eddie.aiida.service;

import energy.eddie.aiida.dto.PermissionDto;
import energy.eddie.aiida.model.permission.Permission;
import energy.eddie.aiida.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {
    private final PermissionRepository repository;

    @Autowired
    public PermissionService(PermissionRepository repository) {
        this.repository = repository;
    }

    public Permission setupNewPermission(PermissionDto dto) {
        Permission newPermission = new Permission(dto.serviceName(), dto.startTime(), dto.expirationTime(),
                dto.grantTime(), dto.connectionId(), dto.requestedCodes(), dto.kafkaStreamingConfig());
        return repository.save(newPermission);
    }

    public List<Permission> getAllPermissionsSortedByGrantTime() {
        return repository.findAllByOrderByGrantTimeDesc();
    }
}
