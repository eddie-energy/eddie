package energy.eddie.regionconnector.at.eda.permission.request.projections;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(schema = "at_eda", name = "meter_reading_timeframe")
public class MeterReadingTimeframe {
    @Id
    @Column(name = "id")
    private final Long id;
    @Column(name = "permission_id")
    private final String permissionId;
    @Column(name = "meter_reading_start")
    private final LocalDate start;
    @Column(name = "meter_reading_end")
    private final LocalDate end;

    public MeterReadingTimeframe(Long id, String permissionId, LocalDate start, LocalDate end) {
        this.id = id;
        this.permissionId = permissionId;
        this.start = start;
        this.end = end;
    }

    @SuppressWarnings("NullAway")
    protected MeterReadingTimeframe() {
        id = null;
        permissionId = null;
        start = null;
        end = null;
    }

    public String permissionId() {
        return permissionId;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(permissionId);
        result = 31 * result + Objects.hashCode(start);
        result = 31 * result + Objects.hashCode(end);
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof MeterReadingTimeframe that)) return false;

        return Objects.equals(permissionId, that.permissionId)
               && Objects.equals(start, that.start)
               && Objects.equals(end, that.end);
    }
}
