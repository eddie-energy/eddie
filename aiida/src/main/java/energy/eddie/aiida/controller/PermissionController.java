package energy.eddie.aiida.controller;

import energy.eddie.aiida.dto.PatchOperation;
import energy.eddie.aiida.dto.PatchPermissionDto;
import energy.eddie.aiida.dto.PermissionDto;
import energy.eddie.aiida.error.InvalidPatchOperationException;
import energy.eddie.aiida.model.permission.Permission;
import energy.eddie.aiida.service.PermissionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import java.util.List;

@RestController
@RequestMapping("/permissions")
public class PermissionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionController.class);
    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissionsSortedByGrantTime());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Permission> setupNewPermission(@Valid @RequestBody PermissionDto newPermission) {
        LOGGER.debug("Got new permission request {}", newPermission);

        var permission = permissionService.setupNewPermission(newPermission);

        var location = new UriTemplate("/permissions/{permissionId}")
                .expand(permission.permissionId());

        return ResponseEntity.created(location).body(permission);
    }

    @PatchMapping(value = "/{permissionId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Permission> revokePermission(@Valid @RequestBody PatchPermissionDto patchPermissionDto,
                                                @PathVariable String permissionId) {
        if (patchPermissionDto.operation() != PatchOperation.REVOKE_PERMISSION) {
            throw new InvalidPatchOperationException();
        }

        return ResponseEntity.ok(permissionService.revokePermission(permissionId));
    }
}
