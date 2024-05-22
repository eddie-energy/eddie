package energy.eddie.admin.console.data;

import jakarta.persistence.*;

@Entity
@Table(name = "status_messages", schema = "admin_console")
public class StatusMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Column(name = "country")
    private final String country;

    @Column(name = "dso")
    private final String dso;

    @Column(name = "permission_id", nullable = false)
    private final String permissionId;

    @Column(name = "start_date")
    private final String startDate;

    @Column(name = "status")
    private final String status;

    // Constructors
    public StatusMessage(String permissionId, String country, String dso, String startDate, String status) {
        this.id = 0L;
        this.permissionId = permissionId;
        this.country = country;
        this.dso = dso;
        this.startDate = startDate;
        this.status = status;
    }

    @SuppressWarnings("NullAway") // Hibernate requires a no-arg constructor
    protected StatusMessage() {
        this.id = null;
        this.permissionId = null;
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