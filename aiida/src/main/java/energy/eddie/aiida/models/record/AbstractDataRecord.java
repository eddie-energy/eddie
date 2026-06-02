// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.DataSource;
import jakarta.persistence.*;

import java.time.Instant;

@MappedSuperclass
public abstract class AbstractDataRecord {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Used just as JPA ID
    @SuppressWarnings({"unused", "NullAway"})
    protected Long id;

    @Column(nullable = false)
    @JsonProperty
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    protected Instant timestamp;

    @ManyToOne
    @JoinColumn(name = "data_source_id", referencedColumnName = "id", nullable = false, updatable = false)
    @JsonIgnore
    protected DataSource dataSource;

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected AbstractDataRecord() {}

    protected AbstractDataRecord(Long id, Instant timestamp, DataSource dataSource) {
        this(timestamp, dataSource);
        this.id = id;
    }

    protected AbstractDataRecord(Instant timestamp, DataSource dataSource) {
        this.timestamp = timestamp;
        this.dataSource = dataSource;
    }

    public Long id() {
        return id;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public DataSource dataSource() {
        return dataSource;
    }
}
