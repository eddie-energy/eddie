package energy.eddie.exampleappbackend.model.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity(name = "permission")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "eddie_permission_id")
    private String eddiePermissionId;

    @Column(name = "eddie_data_need_id")
    private String eddieDataNeedId;

    @Column(name = "eddie_connector_identifier")
    private String eddieConnectorIdentifier;

    @Column(name = "eddie_connector_country")
    private String eddieConnectorCountry;

    @Column(name = "meter_resource_id")
    private String meterResourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type")
    private PermissionType permissionType;

    @OneToOne(mappedBy = "permission", cascade = CascadeType.ALL)
    private TimeSeriesList timeSeriesList;
}
