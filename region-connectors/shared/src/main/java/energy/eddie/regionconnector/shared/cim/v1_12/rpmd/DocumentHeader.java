// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12.rpmd;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v1_12.rpmd.MessageDocumentHeader;
import energy.eddie.cim.v1_12.rpmd.MetaInformation;
import energy.eddie.regionconnector.shared.cim.v1_12.DocumentType;

import java.time.Clock;
import java.time.ZonedDateTime;

public record DocumentHeader(PermissionRequest permissionRequest, DocumentType documentType) {

    public MessageDocumentHeader permissionMarketDocumentHeader(Clock clock) {
        return new MessageDocumentHeader()
                .withCreationDateTime(ZonedDateTime.now(clock))
                .withMetaInformation(
                        new MetaInformation()
                                .withConnectionId(permissionRequest.connectionId())
                                .withDataNeedId(permissionRequest.dataNeedId())
                                .withRequestPermissionId(permissionRequest.permissionId())
                                .withDocumentType(documentType.description())
                                .withRegionConnector(permissionRequest.dataSourceInformation().regionConnectorId())
                                .withRegionCountry(permissionRequest.dataSourceInformation().countryCode())
                );
    }
}
