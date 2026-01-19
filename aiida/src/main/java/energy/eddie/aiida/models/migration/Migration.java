package energy.eddie.aiida.models.migration;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "aiida_migration")
public class Migration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "installed_rank")
    private int installedRank;

    @Column(name = "migration_key")
    private String migrationKey;

    @Column
    private String description;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @SuppressWarnings("NullAway")
    protected Migration() {
    }

    @SuppressWarnings("NullAway")
    public Migration(String migrationKey, String description) {
        this.migrationKey = migrationKey;
        this.description = description;
    }
}
