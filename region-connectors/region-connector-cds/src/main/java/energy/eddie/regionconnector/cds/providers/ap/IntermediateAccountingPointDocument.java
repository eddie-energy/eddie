package energy.eddie.regionconnector.cds.providers.ap;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.cds.providers.cim.Account;
import energy.eddie.regionconnector.cds.providers.cim.CimStruct;
import energy.eddie.regionconnector.cds.providers.cim.ServiceContract;
import energy.eddie.regionconnector.cds.providers.cim.ServicePoint;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

class IntermediateAccountingPointDocument {
    private final IdentifiableAccountingPointData apData;

    IntermediateAccountingPointDocument(IdentifiableAccountingPointData apData) {this.apData = apData;}

    public List<AccountingPointEnvelope> toAp() {
        var accounts = new CimStruct(apData).get();
        var accountingPointDocuments = new ArrayList<AccountingPointEnvelope>();
        var esmpNow = EsmpDateTime.now();
        var codingScheme = CimUtils.getCodingSchemeAp(apData.permissionRequest()
                                                            .dataSourceInformation()
                                                            .countryCode());
        for (var account : accounts) {
            var ap = new AccountingPointMarketDocumentComplexType()
                    .withMRID(UUID.randomUUID().toString())
                    .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                    .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                    .withCreatedDateTime(esmpNow.toString())
                    .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                    .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                    .withSenderMarketParticipantMRID(
                            new PartyIDStringComplexType()
                                    .withCodingScheme(codingScheme)
                                    .withValue("CDSC")
                    )
                    .withReceiverMarketParticipantMRID(
                            new PartyIDStringComplexType()
                                    .withCodingScheme(codingScheme)
                                    .withValue(account.cdsCustomerNumber())
                    )
                    .withAccountingPointList(
                            new AccountingPointMarketDocumentComplexType.AccountingPointList()
                                    .withAccountingPoints(getAccountingPoints(account, codingScheme))
                    );

            accountingPointDocuments.add(new APEnvelope(ap, apData.permissionRequest()).wrap());
        }
        return accountingPointDocuments;
    }

    private Collection<AccountingPointComplexType> getAccountingPoints(
            Account account,
            CodingSchemeTypeList codingScheme
    ) {
        var accountingPoints = new ArrayList<AccountingPointComplexType>();
        var contractParty = getContractParty(account);
        for (var serviceContract : account.serviceContracts()) {
            var invoiceAddress = addressToAddressComplexType(AddressRoleType.INVOICE,
                                                             serviceContract.contractAddress());
            var addresses = getAddressesFromServicePoints(serviceContract.servicePoints());
            addresses.add(invoiceAddress);
            var commodity = getCommodity(serviceContract);
            for (var servicePoint : serviceContract.servicePoints()) {
                for (var meterDevice : servicePoint.meterDevices()) {
                    var ap = new AccountingPointComplexType()
                            .withCommodity(commodity)
                            .withMRID(new MeasurementPointIDStringComplexType()
                                              .withCodingScheme(codingScheme)
                                              .withValue(meterDevice.cdsMeterDeviceId()))
                            .withContractPartyList(new AccountingPointComplexType.ContractPartyList()
                                                           .withContractParties(contractParty))
                            .withAddressList(new AccountingPointComplexType.AddressList().withAddresses(addresses));
                    accountingPoints.add(ap);
                }
            }
        }
        return accountingPoints;
    }

    private static List<AddressComplexType> getAddressesFromServicePoints(List<ServicePoint> servicePoints) {
        var addresses = new ArrayList<AddressComplexType>();
        for (var servicePoint : servicePoints) {
            addresses.add(addressToAddressComplexType(AddressRoleType.DELIVERY, servicePoint.servicePointAddress()));
        }
        return addresses;
    }

    private static AddressComplexType addressToAddressComplexType(AddressRoleType role, String rawAddress) {
        var address = Address.parse(rawAddress);
        return address == null
                ? new AddressComplexType()
                .withAddressRole(role)
                .withAddressSuffix(rawAddress)
                : new AddressComplexType()
                .withAddressRole(role)
                .withPostalCode(address.zip())
                .withCityName(address.city())
                .withStreetName(address.street())
                .withBuildingNumber(address.houseNumber())
                .withStaircaseNumber(address.staircase())
                .withFloorNumber(address.floor())
                .withDoorNumber(address.door())
                .withAddressSuffix(address.suffix());
    }

    private static ContractPartyComplexType getContractParty(Account account) {
        var contractParty = new ContractPartyComplexType()
                .withContractPartyRole(ContractPartyRoleType.CONTRACTPARTNER);
        var accountName = account.accountName();
        if (account.accountType().equals("business")) {
            return contractParty.withCompanyName(accountName);
        }
        var names = accountName.split(" ", -1);
        if(names.length < 2) {
            return contractParty.withSurName(accountName);
        }
        return contractParty
                .withFirstName(accountName.split(" ", -1)[0])
                .withSurName(accountName.split(" ", -1)[1]);
    }

    @Nullable
    private static CommodityKind getCommodity(ServiceContract serviceContract) {
        return switch (serviceContract.serviceType()) {
            case "electric" -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case "natural_gas" -> CommodityKind.NATURALGAS;
            case "water" -> CommodityKind.POTABLEWATER;
            default -> null;
        };
    }
}
