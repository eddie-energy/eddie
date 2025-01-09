package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.eda.EdaResourceLoader;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateAccountingPointMarketDocumentFactoryTest {

    @SuppressWarnings({"java:S5961", "OptionalGetWithoutIsPresent"}) // nr of assertions
    @Test
    void mapsIncomingIdentifiableMasterDataToAccountingPointMarketDocument() throws IOException {
        IntermediateAccountingPointMarketDocumentFactory factory = new IntermediateAccountingPointMarketDocumentFactory(
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                        "fallbackId")
        );

        EdaMasterData edaMasterData = EdaResourceLoader.loadEdaMasterData();
        var created = new EsmpDateTime(edaMasterData.documentCreationDateTime()).toString();

        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "did"
        );
        IdentifiableMasterData identifiableMasterData = new IdentifiableMasterData(
                edaMasterData,
                permissionRequest
        );


        var res = factory.create(identifiableMasterData).accountingPointEnvelope();
        var header = res.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        var md = res.getAccountingPointMarketDocument();
        var ap = md.getAccountingPointList().getAccountingPoints().getFirst();
        var bd = ap.getBillingData();
        var cp = ap.getContractPartyList().getContractParties().getFirst();
        var add = ap.getAddressList().getAddresses().getFirst();
        assertAll(
                () -> assertEquals(permissionRequest.permissionId(), header.getPermissionid()),
                () -> assertEquals(permissionRequest.connectionId(), header.getConnectionid()),
                () -> assertEquals(permissionRequest.dataNeedId(), header.getDataNeedid()),
                () -> assertEquals(edaMasterData.messageId(), md.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), md.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA, md.getType()),
                () -> assertEquals(created, md.getCreatedDateTime()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   md.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.PARTY_CONNECTED_TO_GRID,
                                   md.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   md.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(edaMasterData.senderMessageAddress(),
                                   md.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   md.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(edaMasterData.receiverMessageAddress(),
                                   md.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(1, md.getAccountingPointList().getAccountingPoints().size()),
                () -> assertEquals(edaMasterData.meteringPointData().granularity().name(), ap.getResolution()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED, ap.getCommodity()),
                () -> assertEquals(edaMasterData.meteringPointData().energyCommunity(), ap.getEnergyCommunity()),
                () -> assertEquals(DirectionTypeList.DOWN, ap.getDirection()),
                () -> assertEquals(edaMasterData.meteringPointData().typeOfGeneration(), ap.getGenerationType()),
                () -> assertEquals(edaMasterData.meteringPointData().loadProfileType(), ap.getLoadProfileType()),
                () -> assertEquals(edaMasterData.meteringPointData().supStatus(), ap.getSupplyStatus()),
                () -> assertEquals(edaMasterData.meteringPointData().dsoTariff(), ap.getTariffClassDSO()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   ap.getMRID().getCodingScheme()),
                () -> assertEquals(edaMasterData.meteringPoint(), ap.getMRID().getValue()),
                () -> assertEquals(edaMasterData.billingData().get().referenceNumber(), bd.getReferenceNumber()),
                () -> assertEquals(edaMasterData.billingData().get().gridInvoiceRecipient(),
                                   bd.getGridAgreementTypeDescription()),
                () -> assertEquals(edaMasterData.billingData().get().budgetBillingCycle(), bd.getBudgetBillingCycle()),
                () -> assertEquals(edaMasterData.billingData().get().consumptionBillingCycle(),
                                   bd.getConsumptionBillingCycle()),
                () -> assertEquals(edaMasterData.billingData().get().consumptionBillingMonth(),
                                   bd.getConsumptionBillingMonth()),
                () -> assertEquals(edaMasterData.billingData().get().meterReadingMonth(),
                                   bd.getMeterReadingMonth()),
                () -> assertEquals(edaMasterData.billingData().get().yearMonthOfNextBill(),
                                   bd.getYearMonthOfNextBill()),
                () -> assertEquals(1, ap.getContractPartyList().getContractParties().size()),
                () -> assertEquals(ContractPartyRoleType.CONTRACTPARTNER, cp.getContractPartyRole()),
                () -> assertEquals(edaMasterData.contractPartner().get().salutation(), cp.getSalutation()),
                () -> assertEquals(edaMasterData.contractPartner().get().surname(), cp.getSurName()),
                () -> assertEquals(edaMasterData.contractPartner().get().firstName(), cp.getFirstName()),
                () -> assertNull(cp.getCompanyName()),
                () -> assertEquals(edaMasterData.contractPartner().get().contractPartnerNumber(),
                                   cp.getIdentification()),
                () -> assertEquals(edaMasterData.contractPartner().get().dateOfBirth(), cp.getDateOfBirth()),
                () -> assertEquals(edaMasterData.contractPartner().get().email(), cp.getEmail()),
                () -> assertEquals(edaMasterData.contractPartner().get().dateOfDeath(), cp.getDateOfDeath()),
                () -> assertEquals(edaMasterData.contractPartner().get().companyRegisterNumber(),
                                   cp.getCompanyRegisterNumber()),
                () -> assertEquals(edaMasterData.contractPartner().get().vatNumber(), cp.getVATnumber()),
                () -> assertEquals(1, ap.getAddressList().getAddresses().size()),
                () -> assertEquals(AddressRoleType.DELIVERY, add.getAddressRole()),
                () -> assertEquals(edaMasterData.installationAddress().get().zipCode(), add.getPostalCode()),
                () -> assertEquals(edaMasterData.installationAddress().get().city(), add.getCityName()),
                () -> assertEquals(edaMasterData.installationAddress().get().street(), add.getStreetName()),
                () -> assertEquals(edaMasterData.installationAddress().get().streetNumber(), add.getBuildingNumber()),
                () -> assertEquals(edaMasterData.installationAddress().get().staircase(), add.getStaircaseNumber()),
                () -> assertEquals(edaMasterData.installationAddress().get().floor(), add.getFloorNumber()),
                () -> assertEquals(edaMasterData.installationAddress().get().door(), add.getDoorNumber()),
                () -> assertEquals(edaMasterData.installationAddress().get().addressAddition(), add.getAddressSuffix()),
                () -> assertEquals(1, ap.getAddressList().getAddresses().size())
        );
    }

    @SuppressWarnings({"DataFlowIssue", "java:S5961"}) // nr of assertions
    @Test
    void mapsIncomingIdentifiableMasterDataToAccountingPointMarketDocument_forCompany() throws IOException {
        IntermediateAccountingPointMarketDocumentFactory factory = new IntermediateAccountingPointMarketDocumentFactory(
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                        "fallbackId")
        );

        EdaMasterData edaMasterData = EdaResourceLoader.loadEdaMasterDataForCompany();
        var created = new EsmpDateTime(edaMasterData.documentCreationDateTime()).toString();

        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "did"
        );
        IdentifiableMasterData identifiableMasterData = new IdentifiableMasterData(
                edaMasterData,
                permissionRequest
        );


        var res = factory.create(identifiableMasterData).accountingPointEnvelope();
        var header = res.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        var md = res.getAccountingPointMarketDocument();
        var ap = md.getAccountingPointList().getAccountingPoints().getFirst();
        var bd = ap.getBillingData();
        var cp = ap.getContractPartyList().getContractParties().getFirst();
        var add = ap.getAddressList().getAddresses().getFirst();
        var invoiceCP = ap.getContractPartyList().getContractParties().getLast();
        var invoiceADD = ap.getAddressList().getAddresses().getLast();
        assertAll(
                () -> assertEquals(permissionRequest.permissionId(), header.getPermissionid()),
                () -> assertEquals(permissionRequest.connectionId(), header.getConnectionid()),
                () -> assertEquals(permissionRequest.dataNeedId(), header.getDataNeedid()),
                () -> assertEquals(edaMasterData.messageId(), md.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), md.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA, md.getType()),
                () -> assertEquals(created, md.getCreatedDateTime()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   md.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.PARTY_CONNECTED_TO_GRID,
                                   md.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   md.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(edaMasterData.senderMessageAddress(),
                                   md.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   md.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(edaMasterData.receiverMessageAddress(),
                                   md.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(1, md.getAccountingPointList().getAccountingPoints().size()),
                () -> assertEquals(edaMasterData.meteringPointData().granularity().name(), ap.getResolution()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED, ap.getCommodity()),
                () -> assertEquals(edaMasterData.meteringPointData().energyCommunity(), ap.getEnergyCommunity()),
                () -> assertEquals(DirectionTypeList.UP, ap.getDirection()),
                () -> assertEquals(edaMasterData.meteringPointData().typeOfGeneration(), ap.getGenerationType()),
                () -> assertEquals(edaMasterData.meteringPointData().loadProfileType(), ap.getLoadProfileType()),
                () -> assertEquals(edaMasterData.meteringPointData().supStatus(), ap.getSupplyStatus()),
                () -> assertEquals(edaMasterData.meteringPointData().dsoTariff(), ap.getTariffClassDSO()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   ap.getMRID().getCodingScheme()),
                () -> assertEquals(edaMasterData.meteringPoint(), ap.getMRID().getValue()),
                () -> assertEquals(edaMasterData.billingData().get().referenceNumber(), bd.getReferenceNumber()),
                () -> assertEquals(edaMasterData.billingData().get().gridInvoiceRecipient(),
                                   bd.getGridAgreementTypeDescription()),
                () -> assertEquals(edaMasterData.billingData().get().budgetBillingCycle(), bd.getBudgetBillingCycle()),
                () -> assertEquals(edaMasterData.billingData().get().consumptionBillingCycle(),
                                   bd.getConsumptionBillingCycle()),
                () -> assertEquals(edaMasterData.billingData().get().consumptionBillingMonth(),
                                   bd.getConsumptionBillingMonth()),
                () -> assertEquals(edaMasterData.billingData().get().meterReadingMonth(),
                                   bd.getMeterReadingMonth()),
                () -> assertEquals(edaMasterData.billingData().get().yearMonthOfNextBill(),
                                   bd.getYearMonthOfNextBill()),
                () -> assertEquals(2, ap.getContractPartyList().getContractParties().size()),
                // delivery contract partner
                () -> assertEquals(ContractPartyRoleType.CONTRACTPARTNER, cp.getContractPartyRole()),
                () -> assertEquals(edaMasterData.contractPartner().get().salutation(), cp.getSalutation()),
                () -> assertEquals(edaMasterData.contractPartner().get().companyName(), cp.getCompanyName()),
                () -> assertNull(cp.getFirstName()),
                () -> assertNull(cp.getSurName()),
                () -> assertEquals(edaMasterData.contractPartner().get().contractPartnerNumber(),
                                   cp.getIdentification()),
                () -> assertEquals(edaMasterData.contractPartner().get().dateOfBirth(), cp.getDateOfBirth()),
                () -> assertEquals(edaMasterData.contractPartner().get().email(), cp.getEmail()),
                () -> assertEquals(edaMasterData.contractPartner().get().dateOfDeath(), cp.getDateOfDeath()),
                () -> assertEquals(edaMasterData.contractPartner().get().companyRegisterNumber(),
                                   cp.getCompanyRegisterNumber()),
                () -> assertEquals(edaMasterData.contractPartner().get().vatNumber(), cp.getVATnumber()),
                // invoice contract partner
                () -> assertEquals(ContractPartyRoleType.INVOICE, invoiceCP.getContractPartyRole()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().contractPartner().salutation(),
                                   invoiceCP.getSalutation()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().contractPartner().surname(),
                                   invoiceCP.getSurName()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().contractPartner().firstName(),
                                   invoiceCP.getFirstName()),
                () -> assertNull(invoiceCP.getCompanyName()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().contractPartner().contractPartnerNumber(),
                                   invoiceCP.getIdentification()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().contractPartner().dateOfBirth(),
                                   invoiceCP.getDateOfBirth()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().contractPartner().email(),
                                   invoiceCP.getEmail()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().contractPartner().dateOfDeath(),
                                   invoiceCP.getDateOfDeath()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().contractPartner().companyRegisterNumber(),
                                   invoiceCP.getCompanyRegisterNumber()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().contractPartner().vatNumber(),
                                   invoiceCP.getVATnumber()),
                () -> assertEquals(2, ap.getAddressList().getAddresses().size()),
                // delivery address
                () -> assertEquals(AddressRoleType.DELIVERY, add.getAddressRole()),
                () -> assertEquals(edaMasterData.installationAddress().get().zipCode(), add.getPostalCode()),
                () -> assertEquals(edaMasterData.installationAddress().get().city(), add.getCityName()),
                () -> assertEquals(edaMasterData.installationAddress().get().street(), add.getStreetName()),
                () -> assertEquals(edaMasterData.installationAddress().get().streetNumber(), add.getBuildingNumber()),
                () -> assertEquals(edaMasterData.installationAddress().get().staircase(), add.getStaircaseNumber()),
                () -> assertEquals(edaMasterData.installationAddress().get().floor(), add.getFloorNumber()),
                () -> assertEquals(edaMasterData.installationAddress().get().door(), add.getDoorNumber()),
                () -> assertEquals(edaMasterData.installationAddress().get().addressAddition(), add.getAddressSuffix()),
                // invoice address
                () -> assertEquals(AddressRoleType.INVOICE, invoiceADD.getAddressRole()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().address().zipCode(),
                                   invoiceADD.getPostalCode()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().address().city(), invoiceADD.getCityName()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().address().street(),
                                   invoiceADD.getStreetName()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().address().streetNumber(),
                                   invoiceADD.getBuildingNumber()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().address().staircase(),
                                   invoiceADD.getStaircaseNumber()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().address().floor(),
                                   invoiceADD.getFloorNumber()),
                () -> assertEquals(edaMasterData.invoiceRecipient().get().address().door(), invoiceADD.getDoorNumber())
        );
    }

    @Test
    void testAccountingPointMarketDocument_withoutMeteringPointData_returns() throws IOException {
        // Given
        var factory = new IntermediateAccountingPointMarketDocumentFactory(
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                        "fallbackId")
        );

        var edaMasterData = EdaResourceLoader.loadEdaMasterDataWithoutMeteringPointData();

        var permissionRequest = new SimplePermissionRequest( "pid", "cid", "did" );
        var identifiableMasterData = new IdentifiableMasterData( edaMasterData, permissionRequest );

        // When
        var res = factory.create(identifiableMasterData).accountingPointEnvelope();

        // Then
        var ap = res.getAccountingPointMarketDocument()
                    .getAccountingPointList()
                    .getAccountingPoints()
                    .getFirst();
        assertNull(ap.getDirection());
    }
}
