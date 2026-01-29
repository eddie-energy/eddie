// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth;

import jakarta.annotation.Nullable;

public record Callback(@Nullable String code, @Nullable String error, String state) {
}
