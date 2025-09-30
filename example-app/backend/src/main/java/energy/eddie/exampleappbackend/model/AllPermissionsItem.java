package energy.eddie.exampleappbackend.model;

import energy.eddie.exampleappbackend.model.db.Permission;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AllPermissionsItem {
    private Long id;
    private String type;
    private String name;
    private String status;

    public AllPermissionsItem(Permission permission) {
        this(permission.getId(), permission.getPermissionType().name(), permission.getName(), permission.getStatus());
    }
}
