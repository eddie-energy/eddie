// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.Map;

@Entity
@SuppressWarnings("NullAway")
public class InternalPollingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @ElementCollection
    @MapKeyJoinColumn(name = "permission_id", referencedColumnName = "permission_id")
    @CollectionTable(name = "last_meter_readings", joinColumns = @JoinColumn(name = "permission_id"), schema = "cds")
    private final Map<String, ZonedDateTime> lastMeterReadings;

    public InternalPollingEvent(String permissionId, Map<String, ZonedDateTime> lastMeterReadings) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.lastMeterReadings = lastMeterReadings;
    }

    protected InternalPollingEvent() {
        this(null, null);
    }

    public Map<String, ZonedDateTime> lastMeterReadings() {
        return lastMeterReadings;
    }
}
