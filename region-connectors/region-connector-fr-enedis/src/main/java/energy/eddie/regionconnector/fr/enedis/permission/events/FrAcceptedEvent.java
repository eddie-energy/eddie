// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class FrAcceptedEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String usagePointId;
    // Carries the granularity of the permission request, so handlers don't have to read it from the permission request,
    // which might not be up to date yet when the event is emitted. Null means accounting point data is requested.
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    @Nullable
    private final Granularity granularity;

    public FrAcceptedEvent(String permissionId, String usagePointId, @Nullable Granularity granularity) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.usagePointId = usagePointId;
        this.granularity = granularity;
    }

    protected FrAcceptedEvent() {
        super();
        this.usagePointId = null;
        this.granularity = null;
    }

    public String usagePointId() {
        return usagePointId;
    }

    @Nullable
    public Granularity granularity() {
        return granularity;
    }
}
