package energy.eddie.regionconnector.shared.cim.v0_82.ap;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.ap.AccountingPointMarketDocumentComplexType;
import energy.eddie.cim.v0_82.ap.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.ap.MessageDocumentHeaderRegionComplexType;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;

import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class APEnvelope {
    private final AccountingPointMarketDocumentComplexType ap;
    private final PermissionRequest permissionRequest;

    public APEnvelope(AccountingPointMarketDocumentComplexType ap, PermissionRequest permissionRequest) {
        this.ap = ap;
        this.permissionRequest = permissionRequest;
    }

    public AccountingPointEnvelope wrap() {
        var calendar = DatatypeFactory
                .newDefaultInstance()
                .newXMLGregorianCalendar(LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE));
        var codingScheme = CimUtils.getCodingSchemeAp(permissionRequest.dataSourceInformation().countryCode());
        var header = new energy.eddie.cim.v0_82.ap.MessageDocumentHeaderComplexType()
                .withCreationDateTime(calendar)
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
        return new AccountingPointEnvelope()
                .withMessageDocumentHeader(header)
                .withAccountingPointMarketDocument(ap);
    }
}
