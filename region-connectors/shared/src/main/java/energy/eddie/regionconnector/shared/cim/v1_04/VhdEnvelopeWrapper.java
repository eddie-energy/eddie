package energy.eddie.regionconnector.shared.cim.v1_04;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDMarketDocument;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.shared.cim.v0_82.CimUtils.getCodingSchemeVhdV104;

public class VhdEnvelopeWrapper {
    private final VHDMarketDocument vhdMarketDocument;
    private final PermissionRequest permissionRequest;

    public VhdEnvelopeWrapper(VHDMarketDocument vhdMarketDocument, PermissionRequest permissionRequest) {
        this.vhdMarketDocument = vhdMarketDocument;
        this.permissionRequest = permissionRequest;
    }

    public VHDEnvelope wrap() {
        return new VHDEnvelope()
                .withMarketDocument(vhdMarketDocument)
                .withMessageDocumentHeaderCreationDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withMessageDocumentHeaderMetaInformationDocumentType(DocumentType.VALIDATED_HISTORICAL_DATA_MARKET_DOCUMENT.description())
                .withMessageDocumentHeaderMetaInformationRegionConnector(permissionRequest.dataSourceInformation().regionConnectorId())
                .withMessageDocumentHeaderMetaInformationDataNeedId(permissionRequest.dataNeedId())
                .withMessageDocumentHeaderMetaInformationConnectionId(permissionRequest.connectionId())
                .withMessageDocumentHeaderMetaInformationPermissionId(permissionRequest.permissionId())
                .withMessageDocumentHeaderMetaInformationRegionCountry(getCodingSchemeVhdV104(permissionRequest.dataSourceInformation()
                                                                                                               .countryCode()));
    }
}
