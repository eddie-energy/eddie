package energy.eddie.regionconnector.de.eta.persistence;

import jakarta.persistence.*;
import org.springframework.lang.Nullable; // Ensure this import is present
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(schema = "de_eta", name = "meter_reading_tracking")
@SuppressWarnings("NullAway")
public class DeMeterReadingTrackingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings("UnusedVariable")
    @Nullable // 1. Explicitly mark ID as Nullable since it's null before save
    private Long id;

    @Column(name = "permission_id", nullable = false, unique = true, length = 36)
    private String permissionId;

    @Column(name = "latest_reading_time", nullable = false)
    private Instant latestReadingTime;

    @Column(name = "latest_reading_value")
    @Nullable // 2. Value can be null if the reading is empty
    private BigDecimal latestReadingValue;

    @Column(name = "updated_at", nullable = false)
    @SuppressWarnings("UnusedVariable")
    private Instant updatedAt;

    // JPA Requirement
    @SuppressWarnings("NullAway") // 3. Tell NullAway to ignore this empty constructor
    protected DeMeterReadingTrackingEntity() {
        // No explicit assignments here!
    }

    @SuppressWarnings("NullAway") // 4. Tell NullAway we know ID is not set here
    public DeMeterReadingTrackingEntity(String permissionId, Instant latestReadingTime, BigDecimal latestReadingValue) {
        this.permissionId = permissionId;
        this.latestReadingTime = latestReadingTime;
        this.latestReadingValue = latestReadingValue;
        this.updatedAt = Instant.now();
    }

    public void updateReading(Instant time, BigDecimal value) {
        this.latestReadingTime = time;
        this.latestReadingValue = value;
        this.updatedAt = Instant.now();
    }

    // Getters
    public String getPermissionId() { return permissionId; }
    public Instant getLatestReadingTime() { return latestReadingTime; }
    public BigDecimal getLatestReadingValue() { return latestReadingValue; }
}