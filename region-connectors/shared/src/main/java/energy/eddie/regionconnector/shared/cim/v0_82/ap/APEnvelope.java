package energy.eddie.regionconnector.shared.cim.v0_82.ap;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.ap.AccountingPointMarketDocumentComplexType;
import energy.eddie.cim.v0_82.ap.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.ap.MessageDocumentHeaderRegionComplexType;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class APEnvelope {
    private final AccountingPointMarketDocumentComplexType ap;
    private final PermissionRequest permissionRequest;

    public APEnvelope(AccountingPointMarketDocumentComplexType ap, PermissionRequest permissionRequest) {
        this.ap = ap;
        this.permissionRequest = permissionRequest;
    }

    public AccountingPointEnvelope wrap() {
        var codingScheme = CimUtils.getCodingSchemeAp(permissionRequest.dataSourceInformation().countryCode());
        var header = new energy.eddie.cim.v0_82.ap.MessageDocumentHeaderComplexType()
                .withCreationDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withMessageDocumentHeaderMetaInformation(
                        new MessageDocumentHeaderMetaInformationComplexType()
                                .withConnectionid(permissionRequest.connectionId())
                                .withDataNeedid(permissionRequest.dataNeedId())
                                .withPermissionid(permissionRequest.permissionId())
                                .withDataType(DocumentType.ACCOUNTING_POINT_MARKET_DOCUMENT.description())
                                .withMessageDocumentHeaderRegion(
                                        new MessageDocumentHeaderRegionComplexType()
                                                .withConnector(permissionRequest.dataSourceInformation()
                                                                                .regionConnectorId())
                                                .withCountry(codingScheme)
                                )
                );
        return new AccountingPointEnvelope()
                .withMessageDocumentHeader(header)
                .withAccountingPointMarketDocument(ap);
    }
}
