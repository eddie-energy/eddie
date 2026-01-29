// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.MijnAansluitingDataSourceInformation;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.oldestDateTime;

@Entity
@Table(schema = "nl_mijn_aansluiting", name = "permission_request")
@SuppressWarnings({"NullAway", "unused"})
public class MijnAansluitingPermissionRequest implements MeterReadingPermissionRequest {
    @Id
    private final String permissionId;
    private final String connectionId;
    private final String dataNeedId;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final PermissionProcessStatus status;
    // The verification state for OAuth
    private final String state;
    // The code verifier for the OAuth PKCE code flow
    private final String codeVerifier;
    private final ZonedDateTime created;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyJoinColumn(name = "permission_id", referencedColumnName = "permission_id")
    @CollectionTable(name = "last_meter_readings", joinColumns = @JoinColumn(name = "permission_id"), schema = "nl_mijn_aansluiting")
    private final Map<String, ZonedDateTime> lastMeterReadings = Map.of();
    @Column(name = "house_number")
    @Nullable
    private final String houseNumber;
    @Column(name = "postal_code")
    @Nullable
    private final String postalCode;

    // Needed for JPA
    protected MijnAansluitingPermissionRequest() {
        this(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @SuppressWarnings({"java:S107"}) // Permission Requests require a lot of parameters
    public MijnAansluitingPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            String state,
            String codeVerifier,
            ZonedDateTime created,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            @Nullable String houseNumber,
            @Nullable String postalCode
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.state = state;
        this.codeVerifier = codeVerifier;
        this.created = created;
        this.start = start;
        this.end = end;
        this.granularity = granularity;
        this.houseNumber = houseNumber;
        this.postalCode = postalCode;
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    @Override
    public String dataNeedId() {
        return dataNeedId;
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return new MijnAansluitingDataSourceInformation();
    }

    @Override
    public ZonedDateTime created() {
        return created;
    }

    @Override
    public LocalDate start() {
        return start;
    }

    @Override
    public LocalDate end() {
        return end;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return oldestDateTime(lastMeterReadings.values()).map(ZonedDateTime::toLocalDate);
    }

    public String codeVerifier() {
        return codeVerifier;
    }

    public Granularity granularity() {
        return granularity;
    }

    public Map<String, ZonedDateTime> lastMeterReadings() {
        return lastMeterReadings;
    }

    @Nullable
    public String houseNumber() {
        return houseNumber;
    }

    @Nullable
    public String postalCode() {
        return postalCode;
    }
}
