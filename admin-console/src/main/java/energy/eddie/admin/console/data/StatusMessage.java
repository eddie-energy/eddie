package energy.eddie.admin.console.data;

import jakarta.persistence.*;

@Entity
@Table(name = "status_messages", schema = "admin_console")
public class StatusMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Column(name = "permission_id", nullable = false)
    private final String permissionId;

    @Column(name = "region_connector_id")
    private final String regionConnectorId;

    @Column(name = "data_need_id")
    private final String dataNeedId;

    @Column(name = "country")
    private final String country;

    @Column(name = "dso")
    private final String dso;

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "status")
    private String status;

    // Constructors
    public StatusMessage(String permissionId, String regionConnectorId, String dataNeedId, String country, String dso, String startDate, String status) {
        this.id = 0L;
        this.permissionId = permissionId;
        this.regionConnectorId = regionConnectorId;
        this.dataNeedId = dataNeedId;
        this.country = country;
        this.dso = dso;
        this.startDate = startDate;
        this.status = status;
    }

    @SuppressWarnings("NullAway") // Hibernate requires a no-arg constructor
    protected StatusMessage() {
        this.id = null;
        this.permissionId = null;
        this.regionConnectorId = null;
        this.dataNeedId = null;
        this.country = null;
        this.status = null;
        this.dso = null;
        this.startDate = null;
    }


    public Long getId() {
        return id;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public String getRegionConnectorId() {
        return regionConnectorId;
    }

    public String getDataNeedId() {
        return dataNeedId;
    }

    public String getCountry() {
        return country;
    }

    public String getDso() {
        return dso;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getStatus() {
        return status;
    }

}