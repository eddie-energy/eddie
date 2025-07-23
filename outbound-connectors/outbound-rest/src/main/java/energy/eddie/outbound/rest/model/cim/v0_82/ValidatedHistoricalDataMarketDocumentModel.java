package energy.eddie.outbound.rest.model.cim.v0_82;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "validated_historical_data_marked_document", schema = "rest")
@SuppressWarnings("NullAway")
public class ValidatedHistoricalDataMarketDocumentModel {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final ValidatedHistoricalDataEnvelope payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public ValidatedHistoricalDataMarketDocumentModel(
            ZonedDateTime insertedAt,
            ValidatedHistoricalDataEnvelope payload
    ) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public ValidatedHistoricalDataMarketDocumentModel(ValidatedHistoricalDataEnvelope payload) {
        this(null, payload);
    }

    protected ValidatedHistoricalDataMarketDocumentModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    public ValidatedHistoricalDataEnvelope payload() {
        return payload;
    }
}
