package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.at.eda.dto.masterdata.*;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;

import java.util.Optional;

public class IntermediateAccountingPointMarketDocument {

    private final IdentifiableMasterData identifiableMasterData;
    private final CommonInformationModelConfiguration cimConfig;

    public IntermediateAccountingPointMarketDocument(
            IdentifiableMasterData identifiableMasterData,
            CommonInformationModelConfiguration cimConfig
    ) {
        this.identifiableMasterData = identifiableMasterData;
        this.cimConfig = cimConfig;
    }

    public EddieAccountingPointMarketDocument eddieAccountingPointMarketDocument() {
        return new EddieAccountingPointMarketDocument(
                identifiableMasterData.permissionRequest().connectionId(),
                identifiableMasterData.permissionRequest().permissionId(),
                identifiableMasterData.permissionRequest().dataNeedId(),
                accountingPointMarketDocument(identifiableMasterData.masterData())
        );
    }

    private AccountingPointMarketDocument accountingPointMarketDocument(EdaMasterData edaMasterData) {
        return new AccountingPointMarketDocument()
                .withMRID(edaMasterData.messageId())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withSenderMarketParticipantMRID(new PartyIDStringComplexType()
                                                         .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                                         .withValue(edaMasterData.senderMessageAddress())
                )
                .withReceiverMarketParticipantMRID(new PartyIDStringComplexType()
                                                           .withCodingScheme(CodingSchemeTypeList.fromValue(
                                                                   cimConfig.eligiblePartyNationalCodingScheme()
                                                                            .value())
                                                           )
                                                           .withValue(edaMasterData.receiverMessageAddress())
                )
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.PARTY_CONNECTED_TO_GRID)
                .withAccountingPointList(new AccountingPointMarketDocument.AccountingPointList()
                                                 .withAccountingPoints(accountingPointComplexType(edaMasterData)
                                                 )
                )
                .withCreatedDateTime(new EsmpDateTime(edaMasterData.documentCreationDateTime()).toString()
                );
    }

    private AccountingPointComplexType accountingPointComplexType(EdaMasterData edaMasterData) {
        var accountingPoint = new AccountingPointComplexType()
                .withResolution(edaMasterData.meteringPointData().granularity().name())
                .withCommodity(switch (edaMasterData.sector()) {
                    case ELECTRICITY -> CommodityKind.ELECTRICITYPRIMARYMETERED;
                    case GAS -> CommodityKind.NATURALGAS;
                })
                .withEnergyCommunity(edaMasterData.meteringPointData().energyCommunity())
                .withDirection(switch (edaMasterData.meteringPointData().energyDirection()) {
                    case CONSUMPTION -> DirectionTypeList.DOWN;
                    case GENERATION -> DirectionTypeList.UP;
                })
                .withGenerationType(edaMasterData.meteringPointData().typeOfGeneration())
                .withLoadProfileType(edaMasterData.meteringPointData().loadProfileType())
                .withSupplyStatus(edaMasterData.meteringPointData().supStatus())
                .withTariffClassDSO(edaMasterData.meteringPointData().dsoTariff())
                .withMRID(new MeasurementPointIDStringComplexType()
                                  .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                  .withValue(edaMasterData.meteringPoint())
                );

        Optional<BillingData> billingData = edaMasterData.billingData();
        if (billingData.isPresent()) {
            accountingPoint = accountingPoint.withBillingData(billingData(billingData.get()));
        }

        var contractParties = new AccountingPointComplexType.ContractPartyList();
        var addresses = new AccountingPointComplexType.AddressList();

        Optional<ContractPartner> contractPartner = edaMasterData.contractPartner();
        if (contractPartner.isPresent()) {
            contractParties = contractParties.withContractParties(
                    contractParty(contractPartner.get())
                            .withContractPartyRole(ContractPartyRoleType.CONTRACTPARTNER)
            );
        }

        Optional<DeliveryAddress> deliveryAddress = edaMasterData.installationAddress();
        if (deliveryAddress.isPresent()) {
            var installationAddress = deliveryAddress.get();
            addresses = addresses.withAddresses(addressComplexType(installationAddress)
                                                        .withAddressRole(AddressRoleType.DELIVERY)
                                                        .withAddressSuffix(installationAddress.addressAddition())
            );
        }


        Optional<InvoiceRecipient> invoiceRecipient = edaMasterData.invoiceRecipient();
        if (invoiceRecipient.isPresent()) {
            contractParties = contractParties.withContractParties(
                    contractParty(invoiceRecipient.get().contractPartner())
                            .withContractPartyRole(ContractPartyRoleType.INVOICE)
            );
            addresses = addresses.withAddresses(addressComplexType(invoiceRecipient.get().address())
                                                        .withAddressRole(AddressRoleType.INVOICE)
            );
        }

        accountingPoint = accountingPoint.withContractPartyList(contractParties)
                                         .withAddressList(addresses);

        return accountingPoint;
    }

    private BillingDataComplexType billingData(energy.eddie.regionconnector.at.eda.dto.masterdata.BillingData billingData) {
        return new BillingDataComplexType()
                .withReferenceNumber(billingData.referenceNumber())
                .withGridAgreementTypeDescription(billingData.gridInvoiceRecipient())
                .withBudgetBillingCycle(billingData.budgetBillingCycle())
                .withConsumptionBillingCycle(billingData.consumptionBillingCycle())
                .withMeterReadingMonth((int) billingData.meterReadingMonth())
                .withConsumptionBillingMonth((int) billingData.consumptionBillingMonth())
                .withYearMonthOfNextBill(billingData.yearMonthOfNextBill());
    }

    private ContractPartyComplexType contractParty(ContractPartner contractPartner) {
        var contractPartnerComplexType = new ContractPartyComplexType()
                .withSalutation(contractPartner.salutation())
                .withIdentification(contractPartner.contractPartnerNumber())
                .withDateOfBirth(contractPartner.dateOfBirth())
                .withDateOfDeath(contractPartner.dateOfDeath())
                .withCompanyRegisterNumber(contractPartner.companyRegisterNumber())
                .withVATnumber(contractPartner.vatNumber())
                .withEmail(contractPartner.email());

        if (contractPartner.companyRegisterNumber() == null) {
            // natural person
            contractPartnerComplexType = contractPartnerComplexType
                    .withSurName(contractPartner.surname())
                    .withFirstName(contractPartner.firstName());
        } else {
            // company
            contractPartnerComplexType = contractPartnerComplexType
                    .withCompanyName(contractPartner.companyName());
        }

        return contractPartnerComplexType;
    }

    private static AddressComplexType addressComplexType(Address address) {
        return new AddressComplexType()
                .withPostalCode(address.zipCode())
                .withCityName(address.city())
                .withStreetName(address.street())
                .withBuildingNumber(address.streetNumber())
                .withStaircaseNumber(address.staircase())
                .withFloorNumber(address.floor())
                .withDoorNumber(address.door());
    }
}
