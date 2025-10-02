package energy.eddie.exampleappbackend.model;

import energy.eddie.exampleappbackend.model.db.Permission;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class AllPermissionsItem {
    private Long id;
    private String type;
    private String name;
    private String status;
    private Instant createdAt;
    private String meterResourceId;
    private String eddieConnectorCountry;
    private String eddieConnectorIdentifier;

    public AllPermissionsItem(Permission permission) {
        this(permission.getId(),
             permission.getPermissionType().name(),
             permission.getName(),
             permission.getStatus(),
             permission.getCreatedAt(),
             permission.getMeterResourceId(),
             permission.getEddieConnectorCountry(),
             permission.getEddieConnectorIdentifier()
        );
    }
}
