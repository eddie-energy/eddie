// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.dtos;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoint;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;

import java.util.List;

public record IdentifiableAccountingPointData(
        MijnAansluitingPermissionRequest permissionRequest,
        List<MeteringPoint> payload
) implements IdentifiablePayload<MijnAansluitingPermissionRequest, List<MeteringPoint>> {

}
