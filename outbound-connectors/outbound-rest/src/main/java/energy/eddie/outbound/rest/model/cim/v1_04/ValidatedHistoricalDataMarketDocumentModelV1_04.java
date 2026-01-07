package energy.eddie.outbound.rest.model.cim.v1_04;

import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "validated_historical_data_marked_document_v1_04", schema = "rest")
@SuppressWarnings({"NullAway", "java:S101"})
public class ValidatedHistoricalDataMarketDocumentModelV1_04 implements ModelWithJsonPayload<VHDEnvelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final VHDEnvelope payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public ValidatedHistoricalDataMarketDocumentModelV1_04(
            ZonedDateTime insertedAt,
            VHDEnvelope payload
    ) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public ValidatedHistoricalDataMarketDocumentModelV1_04(VHDEnvelope payload) {
        this(null, payload);
    }

    protected ValidatedHistoricalDataMarketDocumentModelV1_04() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public VHDEnvelope payload() {
        return payload;
    }

    public Long id() {
        return id;
    }
}
