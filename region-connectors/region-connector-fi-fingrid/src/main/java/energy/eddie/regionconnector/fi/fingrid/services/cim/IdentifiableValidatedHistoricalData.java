// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;

import java.util.List;

public record IdentifiableValidatedHistoricalData(FingridPermissionRequest permissionRequest,
                                                  List<TimeSeriesResponse> payload) implements IdentifiablePayload<FingridPermissionRequest, List<TimeSeriesResponse>> {
}
