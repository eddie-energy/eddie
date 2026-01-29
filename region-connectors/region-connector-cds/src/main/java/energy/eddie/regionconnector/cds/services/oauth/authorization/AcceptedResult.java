// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth.authorization;

public record AcceptedResult(String permissionId, String dataNeedId) implements AuthorizationResult {
}
