// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerDataResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;

public record IdentifiableAccountingPointData(FingridPermissionRequest permissionRequest,
                                              CustomerDataResponse payload) implements IdentifiablePayload<FingridPermissionRequest, CustomerDataResponse> {
}
