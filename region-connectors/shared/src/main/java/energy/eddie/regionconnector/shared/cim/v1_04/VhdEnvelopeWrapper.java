// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_04;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDMarketDocument;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import static energy.eddie.regionconnector.shared.cim.v0_82.CimUtils.getCodingSchemeVhdV104;

public class VhdEnvelopeWrapper {
    private final VHDMarketDocument vhdMarketDocument;
    private final PermissionRequest permissionRequest;
    private final Supplier<ZonedDateTime> creationTime;

    public VhdEnvelopeWrapper(VHDMarketDocument vhdMarketDocument, PermissionRequest permissionRequest) {
        this(vhdMarketDocument, permissionRequest, () -> ZonedDateTime.now(ZoneOffset.UTC));
    }

    public VhdEnvelopeWrapper(
            VHDMarketDocument vhdMarketDocument,
            PermissionRequest permissionRequest,
            ZonedDateTime created
    ) {
        this(vhdMarketDocument, permissionRequest, () -> created);
    }

    private VhdEnvelopeWrapper(
            VHDMarketDocument vhdMarketDocument,
            PermissionRequest permissionRequest,
            Supplier<ZonedDateTime> creationTime
    ) {
        this.creationTime = creationTime;
        this.vhdMarketDocument = vhdMarketDocument;
        this.permissionRequest = permissionRequest;
    }

    public VHDEnvelope wrap() {
        return new VHDEnvelope()
                .withMarketDocument(vhdMarketDocument)
                .withMessageDocumentHeaderCreationDateTime(creationTime.get())
                .withMessageDocumentHeaderMetaInformationDocumentType(DocumentType.VALIDATED_HISTORICAL_DATA_MARKET_DOCUMENT.description())
                .withMessageDocumentHeaderMetaInformationRegionConnector(permissionRequest.dataSourceInformation().regionConnectorId())
                .withMessageDocumentHeaderMetaInformationDataNeedId(permissionRequest.dataNeedId())
                .withMessageDocumentHeaderMetaInformationConnectionId(permissionRequest.connectionId())
                .withMessageDocumentHeaderMetaInformationPermissionId(permissionRequest.permissionId())
                .withMessageDocumentHeaderMetaInformationRegionCountry(getCodingSchemeVhdV104(permissionRequest.dataSourceInformation()
                                                                                                               .countryCode()));
    }
}
