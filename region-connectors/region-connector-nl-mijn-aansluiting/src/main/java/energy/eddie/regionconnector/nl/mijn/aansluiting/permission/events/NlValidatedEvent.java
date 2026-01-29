// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class NlValidatedEvent extends NlPermissionEvent {
    @Column(columnDefinition = "text")
    private final String state;
    @Column(columnDefinition = "text")
    private final String codeVerifier;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    @Nullable
    private final Granularity granularity;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;

    public NlValidatedEvent(
            String permissionId,
            String state,
            String codeVerifier,
            @Nullable Granularity granularity,
            LocalDate start,
            LocalDate end
    ) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.state = state;
        this.codeVerifier = codeVerifier;
        this.granularity = granularity;
        this.start = start;
        this.end = end;
    }

    @SuppressWarnings("NullAway.Init")
    protected NlValidatedEvent() {
        super();
        state = null;
        codeVerifier = null;
        granularity = null;
        start = null;
        end = null;
    }

    public String state() {
        return state;
    }

    public String codeVerifier() {
        return codeVerifier;
    }

    public Granularity granularity() {
        return granularity;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }
}
