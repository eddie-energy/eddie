// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.connectionlimit;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * A continuous period in which the measured power of a permission was outside its connection limits.
 * <p>
 * The limit and the document are only the ones that were violated first. If the limit changes while the violation is
 * ongoing, they are not updated. The peak power is only tracked in the direction of the first violation.
 * The measurements and limits are copied, as they may be cleaned up independently of the violation.
 */
@Entity
@Table(name = "connection_limit_violation")
public class ConnectionLimitViolation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings("unused")
    @Nullable
    private Long id;

    @Column(name = "permission_id", nullable = false, updatable = false)
    private UUID permissionId;

    @Column(name = "data_source_id", nullable = false, updatable = false)
    @JsonProperty
    @Schema(description = "ID of the data source that measured the violation.",
            example = "51d0a13e-688a-454d-acab-7a6b2951cde2")
    private UUID dataSourceId;

    @Column(name = "started_at", nullable = false, updatable = false)
    @JsonProperty
    @Schema(description = "Timestamp of the first measurement that violated the limits in UTC format.",
            example = "2026-07-10T08:45:00Z")
    private Instant startedAt;

    @Nullable
    @Column(name = "ended_at")
    @JsonProperty
    @Schema(description = "Timestamp of the first measurement within the limits again in UTC format. Null if the violation is ongoing.",
            example = "2026-07-10T09:00:00Z")
    private Instant endedAt;

    /**
     * Market document of the violated limit, null if a default limit was violated.
     */
    @Nullable
    @Column(name = "document_id", updatable = false)
    @JsonProperty
    @Schema(description = "Market document ID of the limit that was violated first. Null if a default limit was violated.",
            example = "5dc71d7e-e8cd-4403-a3a8-d3c095c97a12")
    private String documentId;

    @Nullable
    @Column(name = "min_limit_kw", updatable = false)
    @JsonProperty
    @Schema(description = "Minimum limit in kW at the start of the violation. Null if unbounded.", example = "3.0")
    private BigDecimal minLimitKw;

    @Nullable
    @Column(name = "max_limit_kw", updatable = false)
    @JsonProperty
    @Schema(description = "Maximum limit in kW at the start of the violation. Null if unbounded.", example = "8.0")
    private BigDecimal maxLimitKw;

    @Column(name = "start_power_kw", nullable = false, updatable = false)
    @JsonProperty
    @Schema(description = "Signed power in kW of the first measurement that violated the limits.", example = "9.1")
    private BigDecimal startPowerKw;

    @Column(name = "peak_power_kw", nullable = false)
    @JsonProperty
    @Schema(description = "Signed power in kW furthest beyond the limit that was violated first.", example = "9.8")
    private BigDecimal peakPowerKw;

    @SuppressWarnings("NullAway.Init")
    protected ConnectionLimitViolation() {
    }

    public ConnectionLimitViolation(
            UUID permissionId,
            UUID dataSourceId,
            Instant startedAt,
            @Nullable String documentId,
            @Nullable BigDecimal minLimitKw,
            @Nullable BigDecimal maxLimitKw,
            BigDecimal startPowerKw
    ) {
        this.permissionId = permissionId;
        this.dataSourceId = dataSourceId;
        this.startedAt = startedAt.truncatedTo(ChronoUnit.MICROS);
        this.documentId = documentId;
        this.minLimitKw = minLimitKw;
        this.maxLimitKw = maxLimitKw;
        this.startPowerKw = startPowerKw;
        this.peakPowerKw = startPowerKw;
    }

    public UUID permissionId() {
        return permissionId;
    }

    public UUID dataSourceId() {
        return dataSourceId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public @Nullable Instant endedAt() {
        return endedAt;
    }

    public @Nullable String documentId() {
        return documentId;
    }

    public @Nullable BigDecimal minLimitKw() {
        return minLimitKw;
    }

    public @Nullable BigDecimal maxLimitKw() {
        return maxLimitKw;
    }

    public BigDecimal startPowerKw() {
        return startPowerKw;
    }

    public BigDecimal peakPowerKw() {
        return peakPowerKw;
    }

    public void end(Instant endedAt) {
        this.endedAt = endedAt.truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Updates the peak power if the measured power is further beyond the limit than any previous measurement.
     *
     * @return true if the peak changed.
     */
    public boolean registerPower(BigDecimal powerKw) {
        var exceededMax = maxLimitKw != null && startPowerKw.compareTo(maxLimitKw) > 0;
        var isNewPeak = exceededMax ? powerKw.compareTo(peakPowerKw) > 0 : powerKw.compareTo(peakPowerKw) < 0;
        if (isNewPeak) {
            peakPowerKw = powerKw;
        }
        return isNewPeak;
    }
}
