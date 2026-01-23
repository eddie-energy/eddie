// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth.revocation;

public sealed interface RevocationResult permits RevocationResult.InvalidRevocationRequest, RevocationResult.ServiceUnavailable, RevocationResult.SuccessfulRevocation {
    record SuccessfulRevocation() implements RevocationResult {}

    record InvalidRevocationRequest(String reason) implements RevocationResult {

        public static final String UNSUPPORTED_TOKEN_TYPE = "unsupported_token_type";

        public boolean isUnsupportedTokenType() {
            return reason.equals(UNSUPPORTED_TOKEN_TYPE);
        }
    }

    record ServiceUnavailable() implements RevocationResult {}
}
