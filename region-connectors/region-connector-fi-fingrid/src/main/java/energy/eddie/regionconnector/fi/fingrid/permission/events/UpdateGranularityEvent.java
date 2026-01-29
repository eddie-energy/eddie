// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity(name = "FiUpdateGranularityEvent")
@SuppressWarnings({"NullAway", "unused"})
public class UpdateGranularityEvent extends PersistablePermissionEvent implements InternalPermissionEvent {

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;

    public UpdateGranularityEvent(String permissionId, Granularity granularity) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.granularity = granularity;
    }

    protected UpdateGranularityEvent() {
        granularity = null;
    }

    public Granularity granularity() {
        return granularity;
    }
}
