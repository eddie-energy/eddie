// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.persistence;

import io.swagger.v3.oas.annotations.media.Schema;

public interface DataNeedsNameAndIdProjection {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String getId();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String getName();
}
