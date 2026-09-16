// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import java.time.LocalDate;

public record EdaCMRevokeImpl(
        String meteringPoint,
        String consentId,
        LocalDate consentEnd
) implements EdaCMRevoke {
}
