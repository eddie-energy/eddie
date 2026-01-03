package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.es.datadis.ContractDetailsProvider;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.PointType;
import energy.eddie.regionconnector.es.datadis.SupplyProvider;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
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
    public static IdentifiableAccountingPointData identifiableAccountingPointData() throws IOException {
        EsPermissionRequest permissionRequest = new DatadisPermissionRequestBuilder()
                .setPermissionId("permissionId")
                .setConnectionId("connectionId")
                .setDataNeedId("dataNeedId")
                .setNif("nif")
                .setMeteringPointId("meteringPointId")
                .setStart(LocalDate.now(ZONE_ID_SPAIN))
                .setEnd(LocalDate.now(ZONE_ID_SPAIN))
                .setDistributorCode(DistributorCode.IDE)
                .setPointType(PointType.TYPE_1)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                .build();
        return new IdentifiableAccountingPointData(
                permissionRequest,
                new AccountingPointData(
                        SupplyProvider.loadSupply().getFirst(),
                        ContractDetailsProvider.loadContractDetails().getFirst()
                )
        );
    }

    @Test
    @SuppressWarnings("java:S5961")
        // too many assertions
    void accountingPointEnvelope_mapsAsExpected() throws IOException {
        // Given
        DatadisConfiguration datadisConfig = new DatadisConfiguration("clientId", "clientSecret", "basepath");
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
        var res = intermediateAccountingPointMarketDocument.accountingPointEnvelope();

        // Then
        var md = res.getAccountingPointMarketDocument();
        var accountingPoint = md.getAccountingPointList()
                                .getAccountingPoints()
                                .getFirst();
        AddressComplexType address = accountingPoint
                .getAddressList()
                .getAddresses()
                .getFirst();
        BillingDataComplexType billingData = accountingPoint
                .getBillingData();

        var header = res.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        assertAll(
                () -> assertEquals(identifiableAccountingPointData.permissionRequest().permissionId(),
                                   header.getPermissionid()),
                () -> assertEquals(identifiableAccountingPointData.permissionRequest().dataNeedId(),
                                   header.getDataNeedid()),
                () -> assertEquals(identifiableAccountingPointData.permissionRequest().connectionId(),
                                   header.getConnectionid()),
                () -> assertNotNull(res.getAccountingPointMarketDocument()),
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
}
