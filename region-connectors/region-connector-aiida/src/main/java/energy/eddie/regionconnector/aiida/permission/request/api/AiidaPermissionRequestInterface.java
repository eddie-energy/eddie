// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.permission.request.api;


import energy.eddie.api.agnostic.process.model.PermissionRequest;
import jakarta.annotation.Nullable;

import java.util.UUID;

public interface AiidaPermissionRequestInterface extends PermissionRequest {
    /**
     * Optional pyhiscal meter ID provided by the eligible party to map flexible connection agreements and control data source selection.
     */
    @Nullable
    String meterId();

    /**
     * A message providing further information about the latest status.
     */
    @Nullable
    String message();

    /**
     * The unique identifier of the AIIDA application to which the permission belongs.
     */
    @Nullable
    UUID aiidaId();
}
