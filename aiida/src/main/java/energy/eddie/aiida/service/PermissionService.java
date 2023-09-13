package energy.eddie.aiida.service;

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

    public List<Permission> getAllPermissionsSortedByGrantTime() {
        return repository.findAllByOrderByGrantTimeDesc();
    }
}
