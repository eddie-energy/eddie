// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * This event is only used for the internal polling mechanism. It includes the last polled timestamp and the last meter
 * reading.
 */
@Entity
@SuppressWarnings("NullAway")
public class NlInternalPollingEvent extends NlPermissionEvent implements InternalPermissionEvent {

    @ElementCollection
    @MapKeyJoinColumn(name = "permission_id", referencedColumnName = "permission_id")
    @CollectionTable(name = "last_meter_readings", joinColumns = @JoinColumn(name = "permission_id"), schema = "nl_mijn_aansluiting")
    private final Map<String, ZonedDateTime> lastMeterReadings;

    public NlInternalPollingEvent() {
        this(null, null);
    }

    public NlInternalPollingEvent(String permissionId, Map<String, ZonedDateTime> lastMeterReadings) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.lastMeterReadings = lastMeterReadings;
    }

    public Map<String, ZonedDateTime> lastMeterReadings() {
        return lastMeterReadings;
    }
}
