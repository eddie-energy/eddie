// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import jakarta.annotation.Nullable;

public interface DkEnerginetPermissionRequest extends MeterReadingPermissionRequest {

    String refreshToken();

    String accessToken();

    Granularity granularity();

    String meteringPoint();

    @Nullable
    String errors();
}
