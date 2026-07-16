// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

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

    @SuppressWarnings("NullAway.Init")
    protected Migration() {
    }

    @SuppressWarnings("NullAway.Init")
    public Migration(String migrationKey, String description) {
        this.migrationKey = migrationKey;
        this.description = description;
    }
}
