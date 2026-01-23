// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity(name = "BeAcceptedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class AcceptedEvent extends PersistablePermissionEvent {
    @OneToMany(targetEntity = MeterReading.class, cascade = CascadeType.PERSIST)
    @JoinColumn(updatable = false, name = "permission_id", referencedColumnName = "permission_id")
    private final List<MeterReading> meterReadings;

    public AcceptedEvent(String permissionId, List<MeterReading> meterReadings) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.meterReadings = meterReadings;
    }

    public AcceptedEvent() {
        meterReadings = List.of();
    }
}
