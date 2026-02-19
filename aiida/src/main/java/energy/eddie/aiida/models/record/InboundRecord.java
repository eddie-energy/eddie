// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.record.InboundRecordDto;
import energy.eddie.aiida.models.datasource.DataSource;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class InboundRecord {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Used just as JPA ID
    @SuppressWarnings({"unused", "NullAway"})
    private Long id;
    @Column(nullable = false)
    @JsonProperty
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    protected Instant timestamp;
    @ManyToOne
    @JoinColumn(name = "data_source_id", referencedColumnName = "id", nullable = false, updatable = false)
    @JsonIgnore
    private DataSource dataSource;
    @Column(nullable = false)
    @JsonProperty
    private String payload;

    public InboundRecord(
            Instant timestamp,
            DataSource dataSource,
            String payload
    ) {
        this.timestamp = timestamp;
        this.dataSource = dataSource;
        this.payload = payload;
    }

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected InboundRecord() {
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

    public String payload() {
        return payload;
    }

    public InboundRecordDto toDto() {
        return new InboundRecordDto(timestamp,
                                    dataSource.userId(),
                                    dataSource.id(),
                                    dataSource.asset(),
                                    dataSource.meterId(),
                                    dataSource.operatorId(),
                                    payload);
    }
}
