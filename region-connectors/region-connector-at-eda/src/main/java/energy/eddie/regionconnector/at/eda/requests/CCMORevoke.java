// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public record CCMORevoke(AtPermissionRequest permissionRequest, String eligiblePartyId, String reason) {
}
