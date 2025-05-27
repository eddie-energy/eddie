package energy.eddie.regionconnector.shared.cim.v0_82.pmd;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderComplexType;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderRegionComplexType;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record DocumentHeader(PermissionRequest permissionRequest, DocumentType documentType) {

    public MessageDocumentHeaderComplexType permissionMarketDocumentHeader() {
        var codingScheme = CimUtils.getCodingSchemePmd(permissionRequest.dataSourceInformation().countryCode());
        return new MessageDocumentHeaderComplexType()
                .withCreationDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withMessageDocumentHeaderMetaInformation(
                        new MessageDocumentHeaderMetaInformationComplexType()
                                .withConnectionid(permissionRequest.connectionId())
                                .withDataNeedid(permissionRequest.dataNeedId())
                                .withPermissionid(permissionRequest.permissionId())
                                .withDataType(documentType.description())
                                .withMessageDocumentHeaderRegion(
                                        new MessageDocumentHeaderRegionComplexType()
                                                .withConnector(permissionRequest.dataSourceInformation()
                                                                                .regionConnectorId())
                                                .withCountry(codingScheme)
                                )
                );
    }
}
