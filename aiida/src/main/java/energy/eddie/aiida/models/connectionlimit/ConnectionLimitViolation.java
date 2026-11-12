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
 * ongoing, they are not updated, so comparing the extreme powers with these limits is an approximation.
 * <p>
 * Only measurements that violated the limits are tracked, measurements back within the limits are not. As a violation
 * can alternate between exceeding the maximum and falling below the minimum, the lowest and the highest violating
 * measurement are both kept. For a violation that stays on one side, the opposite extreme is its least severe
 * measurement.
 * <p>
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

    @Column(name = "lowest_power_kw", nullable = false)
    @JsonProperty
    @Schema(description = "Lowest signed power in kW measured while violating the limits.", example = "-2.4")
    private BigDecimal lowestPowerKw;

    @Column(name = "highest_power_kw", nullable = false)
    @JsonProperty
    @Schema(description = "Highest signed power in kW measured while violating the limits.", example = "9.8")
    private BigDecimal highestPowerKw;

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
        this.lowestPowerKw = startPowerKw;
        this.highestPowerKw = startPowerKw;
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

    public BigDecimal lowestPowerKw() {
        return lowestPowerKw;
    }

    public BigDecimal highestPowerKw() {
        return highestPowerKw;
    }

    public void end(Instant endedAt) {
        this.endedAt = endedAt.truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Tracks a measurement that violates the limits.
     *
     * @return true if it is lower or higher than any violating measurement before.
     */
    public boolean registerPower(BigDecimal powerKw) {
        var isNewLowest = powerKw.compareTo(lowestPowerKw) < 0;
        var isNewHighest = powerKw.compareTo(highestPowerKw) > 0;
        if (isNewLowest) {
            lowestPowerKw = powerKw;
        }
        if (isNewHighest) {
            highestPowerKw = powerKw;
        }
        return isNewLowest || isNewHighest;
    }
}
