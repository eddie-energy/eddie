package energy.eddie.regionconnector.shared.cim.v0_82.vhd;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderRegionComplexType;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record VhdEnvelope(ValidatedHistoricalDataMarketDocumentComplexType vhd, PermissionRequest permissionRequest) {
    public ValidatedHistoricalDataEnvelope wrap() {
        var codingScheme = CimUtils.getCodingSchemeVhd(permissionRequest.dataSourceInformation().countryCode());
        var header = new energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderComplexType()
                .withCreationDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withMessageDocumentHeaderMetaInformation(
                        new MessageDocumentHeaderMetaInformationComplexType()
                                .withConnectionid(permissionRequest.connectionId())
                                .withDataNeedid(permissionRequest.dataNeedId())
                                .withPermissionid(permissionRequest.permissionId())
                                .withDataType(DocumentType.VALIDATED_HISTORICAL_DATA_MARKET_DOCUMENT.description())
                                .withMessageDocumentHeaderRegion(
                                        new MessageDocumentHeaderRegionComplexType()
                                                .withConnector(permissionRequest.dataSourceInformation()
                                                                                .regionConnectorId())
                                                .withCountry(codingScheme)
                                )
                );
        return new ValidatedHistoricalDataEnvelope()
                .withMessageDocumentHeader(header)
                .withValidatedHistoricalDataMarketDocument(vhd);
    }
}
