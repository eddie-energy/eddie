package energy.eddie.regionconnector.shared.cim.v0_82.pmd;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderComplexType;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderRegionComplexType;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;

import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record DocumentHeader(PermissionRequest permissionRequest, DocumentType documentType) {

    public MessageDocumentHeaderComplexType permissionMarketDocumentHeader() {

        var calendar = DatatypeFactory
                .newDefaultInstance()
                .newXMLGregorianCalendar(LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE));
        var codingScheme = CimUtils.getCodingSchemePmd(permissionRequest.dataSourceInformation().countryCode());
        return new MessageDocumentHeaderComplexType()
                .withCreationDateTime(calendar)
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
