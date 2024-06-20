package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.es.datadis.ContractDetailsProvider;
import energy.eddie.regionconnector.es.datadis.SupplyProvider;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;

class IntermediateAccountingPointMarketDocumentTest {
    @Test
    @SuppressWarnings("java:S5961")
        // too many assertions
    void eddieAccountingPointMarketDocument_mapsAsExpected() throws IOException {
        // Given
        PlainDatadisConfiguration datadisConfig = new PlainDatadisConfiguration("clientId", "clientSecret", "basepath");
        PlainCommonInformationModelConfiguration cimConfiguration = new PlainCommonInformationModelConfiguration(
                energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                "fallbackId");

        IdentifiableAccountingPointData identifiableAccountingPointData = identifiableAccountingPointData();
        IntermediateAccountingPointMarketDocument intermediateAccountingPointMarketDocument = new IntermediateAccountingPointMarketDocument(
                identifiableAccountingPointData,
                cimConfiguration,
                datadisConfig
        );

        // When
        var res = intermediateAccountingPointMarketDocument.eddieAccountingPointMarketDocument();

        // Then
        EsPermissionRequest permissionRequest = identifiableAccountingPointData.permissionRequest();
        AccountingPointMarketDocument md = res.marketDocument();
        AccountingPointComplexType accountingPoint = md.getAccountingPointList()
                                                       .getAccountingPoints()
                                                       .getFirst();
        AddressComplexType address = accountingPoint
                .getAddressList()
                .getAddresses()
                .getFirst();
        BillingDataComplexType billingData = accountingPoint
                .getBillingData();

        assertAll(
                () -> assertEquals(permissionRequest.permissionId(), res.permissionId()),
                () -> assertEquals(permissionRequest.dataNeedId(), res.dataNeedId()),
                () -> assertEquals(permissionRequest.connectionId(), res.connectionId()),
                () -> assertNotNull(md),
                // meta data
                () -> assertNotNull(md.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), md.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA, md.getType()),
                () -> assertNotNull(md.getCreatedDateTime()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   md.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER, md.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(DistributorCode.IDE.name(), md.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                   md.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(datadisConfig.username(), md.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   md.getReceiverMarketParticipantMRID().getCodingScheme()),
                // accounting point
                () -> assertEquals(1, md.getAccountingPointList().getAccountingPoints().size()),
                () -> assertEquals(Granularity.PT1H.name(), accountingPoint.getMeterReadingResolution()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED, accountingPoint.getCommodity()),
                () -> assertEquals(DirectionTypeList.DOWN, accountingPoint.getDirection()),
                () -> assertEquals("3T", accountingPoint.getTariffClassDSO()),
                () -> assertEquals("ES00XXXXXXXXXXXXXXXX", accountingPoint.getMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME,
                                   accountingPoint.getMRID().getCodingScheme()),
                // billing
                () -> assertEquals("BAJA TENSION Y POTENCIA  > 15 kW", billingData.getGridAgreementTypeDescription()),
                // address
                () -> assertEquals(AddressRoleType.DELIVERY, address.getAddressRole()),
                () -> assertEquals("28046", address.getPostalCode()),
                () -> assertEquals("Pseo STREET", address.getStreetName()),
                () -> assertEquals("101", address.getBuildingNumber()),
                () -> assertEquals("14", address.getFloorNumber()),
                () -> assertEquals("2", address.getDoorNumber()),
                () -> assertEquals("MADRID", address.getCityName()),
                () -> assertEquals("MADRID", address.getAddressSuffix())
        );
    }

    public static IdentifiableAccountingPointData identifiableAccountingPointData() throws IOException {
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest(
                "permissionId",
                "connectionId",
                "dataNeedId",
                null,
                "nif",
                "meteringPointId",
                LocalDate.now(ZONE_ID_SPAIN),
                LocalDate.now(ZONE_ID_SPAIN),
                DistributorCode.IDE,
                1,
                null,
                PermissionProcessStatus.ACCEPTED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC),
                null);
        return new IdentifiableAccountingPointData(
                permissionRequest,
                new AccountingPointData(
                        SupplyProvider.loadSupply().getFirst(),
                        ContractDetailsProvider.loadContractDetails().getFirst()
                )
        );
    }
}
