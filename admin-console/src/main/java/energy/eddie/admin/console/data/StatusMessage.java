package energy.eddie.admin.console.data;

import jakarta.persistence.*;

@Entity
@Table(name = "status_messages", schema = "admin_console")
public class StatusMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Column(nullable = false)
    private String permissionId; // is properties.permissionList.permissions.permissionMRID in the database

    @Column(name = "timestamp")
    private String timestamps; // properties.permissionList.permissions.mktActivityRecordList.createdDateTime in database

    @Column(name = "status") // Define the column name for individual elements
    private String status; //properties.permissionList.permissions.mktActivityRecords.status in database

    // Constructors
    public StatusMessage(String permissionId, String timestamps, String status) {
        this.id = 0L;
        this.permissionId = permissionId;
        this.timestamps = timestamps;
        this.status = status;
    }

    @SuppressWarnings("NullAway") // Hibernate requires a no-arg constructor
    public StatusMessage() {
        this.id = null;
        this.permissionId = null;
        this.timestamps = null;
        this.status = null;
    }

    public Long getId() {
        return id;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }

    public String getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(String timestamps) {
        this.timestamps = timestamps;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


}