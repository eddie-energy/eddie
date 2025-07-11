package energy.eddie.exampleappbackend.controller;

import energy.eddie.exampleappbackend.model.PermissionIdTypeAndName;
import energy.eddie.exampleappbackend.model.db.Permission;
import energy.eddie.exampleappbackend.service.PermissionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@AllArgsConstructor
@RequestMapping("/permissions")
@Controller
public class PermissionController {
    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<PermissionIdTypeAndName>> getAllPermissionsForUser() {
        return ResponseEntity.ok(permissionService.getAllPermissionIdAndNameForUser());
    }

    @GetMapping("/{permissionId}")
    public ResponseEntity<Permission> getPermissionById(@PathVariable("permissionId") Long permissionId) {
        var permission = permissionService.getPermissionById(permissionId);
        return permission
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
