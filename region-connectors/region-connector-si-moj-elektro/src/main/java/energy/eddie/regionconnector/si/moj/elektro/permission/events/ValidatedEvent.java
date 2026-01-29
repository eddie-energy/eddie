// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.si.moj.elektro.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity(name = "SiValidatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class ValidatedEvent extends PersistablePermissionEvent {

    @Column(name = "permission_start")
    private LocalDate start;
    @Column(name = "permission_end")
    private LocalDate end;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private Granularity granularity;
    private String apiToken;

    public ValidatedEvent(
            String permissionId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            String apiToken
    ) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.start = start;
        this.end = end;
        this.granularity = granularity;
        this.apiToken = apiToken;
    }

    protected ValidatedEvent() { }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    public Granularity granularity() {
        return granularity;
    }

    public String apiToken() {
        return apiToken;
    }
}
