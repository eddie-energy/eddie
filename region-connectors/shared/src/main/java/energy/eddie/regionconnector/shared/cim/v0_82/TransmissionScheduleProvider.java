// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v0_82;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import jakarta.annotation.Nullable;

public interface TransmissionScheduleProvider<T extends PermissionRequest> {
    @Nullable
    String findTransmissionSchedule(T permissionRequest);
}
