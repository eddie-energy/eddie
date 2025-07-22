package energy.eddie.outbound.rest.model;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "connection_status_message", schema = "rest")
@SuppressWarnings("NullAway")
public class ConnectionStatusMessageModel {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final ConnectionStatusMessage payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public ConnectionStatusMessageModel(
            ZonedDateTime insertedAt,
            ConnectionStatusMessage payload
    ) {
        this.id = null;
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public ConnectionStatusMessageModel(
            ConnectionStatusMessage payload
    ) {
        this(null, payload);
    }

    protected ConnectionStatusMessageModel() {
        this.id = null;
        this.insertedAt = null;
        this.payload = null;
    }

    public ConnectionStatusMessage payload() {
        return payload;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(insertedAt);
        result = 31 * result + Objects.hashCode(payload);
        result = 31 * result + Objects.hashCode(id);
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ConnectionStatusMessageModel that)) return false;

        return Objects.equals(insertedAt, that.insertedAt)
               && Objects.equals(payload, that.payload)
               && Objects.equals(id, that.id);
    }
}
