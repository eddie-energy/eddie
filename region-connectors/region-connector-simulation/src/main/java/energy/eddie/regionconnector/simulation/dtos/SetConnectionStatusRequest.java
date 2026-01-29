// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.dtos;

import energy.eddie.api.v0.PermissionProcessStatus;
import reactor.util.annotation.Nullable;

public class SetConnectionStatusRequest {
    @Nullable
    public String connectionId;
    @Nullable
    public String dataNeedId;
    @Nullable
    public String permissionId;
    @Nullable
    public PermissionProcessStatus connectionStatus;
}