package energy.eddie.regionconnector.fi.fingrid.services;


import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.fi.fingrid.client.model.Address;
import energy.eddie.regionconnector.fi.fingrid.client.model.Agreements;
import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerDataResponse;
import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerTransaction;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

class IntermediateAccountingPointDataMarketDocument {
    private final CustomerDataResponse customerDataResponse;

    IntermediateAccountingPointDataMarketDocument(CustomerDataResponse customerDataResponse) {
        this.customerDataResponse = customerDataResponse;
    }

    public AccountingPointMarketDocumentComplexType toAp() {
        var header = customerDataResponse.customerData().header();
        var created = new EsmpDateTime(header.creation());
        return new AccountingPointMarketDocumentComplexType()
                .withMRID(header.identification())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                .withCreatedDateTime(created.toString())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME)
                                .withValue(header.juridicalSenderParty().identification())
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME)
                                .withValue(header.juridicalReceiverParty().identification())
                )
                .withAccountingPointList(
                        new AccountingPointMarketDocumentComplexType.AccountingPointList()
                                .withAccountingPoints(createAccountingPoints())
                );
    }

    private List<AccountingPointComplexType> createAccountingPoints() {
        var accountingPoints = new ArrayList<AccountingPointComplexType>();
        for (var agreement : customerDataResponse.customerData().transaction().agreements()) {
            var acc = createAccountingPoint(agreement);
            accountingPoints.add(acc);
        }
        return accountingPoints;
    }

    private AccountingPointComplexType createAccountingPoint(Agreements agreement) {
        var invoiceAddress = customerDataResponse.customerData()
                                                 .transaction()
                                                 .customerPostalAddress();
        return new AccountingPointComplexType()
                .withMRID(
                        new MeasurementPointIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME)
                                .withValue(agreement.meteringPoint().meteringPointEAN())
                )
                .withContractPartyList(
                        new AccountingPointComplexType.ContractPartyList()
                                .withContractParties(createContractParty())
                )
                .withAddressList(
                        new AccountingPointComplexType.AddressList()
                                .withAddresses(
                                        createAddress(agreement.meteringPoint().meteringPointAddress(),
                                                      AddressRoleType.DELIVERY),
                                        createAddress(invoiceAddress, AddressRoleType.INVOICE)
                                )
                );
    }


    private ContractPartyComplexType createContractParty() {
        var transaction = customerDataResponse.customerData().transaction();
        var dateOfBirth = getDateOfBirth(transaction);
        return new ContractPartyComplexType()
                .withContractPartyRole(ContractPartyRoleType.CONTRACTPARTNER)
                .withCompanyName(transaction.companyName())
                .withIdentification(transaction.customerIdentification())
                .withDateOfBirth(dateOfBirth)
                .withEmail(transaction.emailAddress());
    }

    @Nullable
    private static XMLGregorianCalendar getDateOfBirth(CustomerTransaction transaction) {
        if (transaction.dateOfBirth() == null) {
            return null;
        }
        return DatatypeFactory.newDefaultInstance()
                              .newXMLGregorianCalendar(GregorianCalendar.from(transaction.dateOfBirth()));
    }

    private static AddressComplexType createAddress(Address address, AddressRoleType role) {
        return new AddressComplexType()
                .withAddressRole(role)
                .withPostalCode(address.postalCode())
                .withCityName(address.postOffice())
                .withStreetName(address.streetName())
                .withBuildingNumber(address.buildingNumber())
                .withStaircaseNumber(address.stairwellIdentification())
                .withDoorNumber(address.apartment())
                .withAddressSuffix(address.addressNote());
    }
}
