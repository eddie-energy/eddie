package energy.eddie.exampleappbackend.model;

import energy.eddie.exampleappbackend.model.db.Permission;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PermissionIdTypeAndName {
    private Long id;
    private String type;
    private String name;

    public PermissionIdTypeAndName(Permission permission) {
        this(permission.getId(), permission.getPermissionType().name(), permission.getName());
    }
}
