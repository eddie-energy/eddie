// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v0_82;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.time.format.DateTimeParseException;

public interface TransmissionScheduleProvider<T extends PermissionRequest> {
    @Nullable
    String findTransmissionSchedule(T permissionRequest);

    @Nullable
    default Duration findTransmissionScheduleDuration(T permissionRequest) {
        var schedule = findTransmissionSchedule(permissionRequest);
        try {
            return schedule == null ? null : Duration.parse(schedule);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
