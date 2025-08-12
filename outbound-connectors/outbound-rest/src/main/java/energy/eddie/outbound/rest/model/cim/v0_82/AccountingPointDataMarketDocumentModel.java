package energy.eddie.outbound.rest.model.cim.v0_82;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "accounting_point_data_market_document", schema = "rest")
@SuppressWarnings("NullAway")
public class AccountingPointDataMarketDocumentModel implements ModelWithJsonPayload<AccountingPointEnvelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final AccountingPointEnvelope payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public AccountingPointDataMarketDocumentModel(
            ZonedDateTime insertedAt,
            AccountingPointEnvelope payload
    ) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public AccountingPointDataMarketDocumentModel(AccountingPointEnvelope payload) {
        this(null, payload);
    }

    protected AccountingPointDataMarketDocumentModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public AccountingPointEnvelope payload() {
        return payload;
    }
}
