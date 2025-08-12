package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;
import energy.eddie.regionconnector.us.green.button.atom.feed.Query;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.Unmarshaller;
import org.naesb.espi.customer.*;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.function.Function;

class IntermediateAccountingPointMarketDocument {
    private final UsGreenButtonPermissionRequest permissionRequest;
    private final SyndFeed feed;
    private final Unmarshaller unmarshaller;

    IntermediateAccountingPointMarketDocument(
            IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> id,
            Jaxb2Marshaller marshaller
    ) {
        this.permissionRequest = id.permissionRequest();
        this.feed = id.payload();
        this.unmarshaller = marshaller.createUnmarshaller();
    }

    List<AccountingPointEnvelope> toAps() {
        var query = new Query(feed, unmarshaller);
        var customers = query.findAllByTitle("Customer");
        var aps = new ArrayList<AccountingPointEnvelope>();
        for (var entry : customers) {
            var customer = query.unmarshal(entry, Customer.class);
            if (customer == null) {
                continue;
            }
            var ap = new AccountingPointMarketDocumentComplexType()
                    .withMRID(UUID.randomUUID().toString())
                    .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                    .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                    .withCreatedDateTime(EsmpDateTime.now().toString())
                    .withSenderMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                    .withReceiverMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                    .withSenderMarketParticipantMRID(
                            new PartyIDStringComplexType()
                                    .withCodingScheme(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME)
                                    .withValue(customer.getCustomerName())
                    )
                    .withReceiverMarketParticipantMRID(
                            new PartyIDStringComplexType()
                                    .withCodingScheme(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME)
                                    .withValue("GreenButton")
                    )
                    .withAccountingPointList(new AccountingPointMarketDocumentComplexType.AccountingPointList()
                                                     .withAccountingPoints(createAccountingPoints(query,
                                                                                                  customer,
                                                                                                  entry)));

            aps.add(new APEnvelope(ap, permissionRequest).wrap());
        }
        return aps;
    }

    private List<AccountingPointComplexType> createAccountingPoints(
            Query query,
            Customer customer,
            SyndEntry customerEntry
    ) {
        var accountingPoints = new ArrayList<AccountingPointComplexType>();
        var codingScheme = CimUtils.getCodingSchemeAp(permissionRequest.dataSourceInformation().countryCode());
        var customerAccounts = query.findEveryRelatedOfType(customerEntry, "cust-feed/CustomerAccount");
        for (var customerAccountEntry : customerAccounts) {
            var customerAccount = query.unmarshal(customerAccountEntry, CustomerAccount.class);
            Function<CustomerAccount, Organisation> acc = CustomerAccount::getContactInfo;
            var customerAccountAddress = addressOfNullable(customerAccount, acc.andThen(this::addressOfOrganisation));
            var agreements = query.findEveryRelatedOfType(customerAccountEntry, "cust-feed/CustomerAgreement");
            for (var agreementEntry : agreements) {
                var agreement = query.unmarshal(agreementEntry, CustomerAgreement.class);
                var serviceLocations = query.findEveryRelatedOfType(agreementEntry, "cust-feed/ServiceLocation");
                for (var serviceLocationEntry : serviceLocations) {
                    var serviceLocation = query.unmarshal(serviceLocationEntry, ServiceLocation.class);
                    var locationAddress = addressOfNullable(serviceLocation, ServiceLocation::getMainAddress);
                    var usagePoints = flattenUsagePoints(serviceLocation);
                    for (var usagePoint : usagePoints) {
                        accountingPoints.add(createAccountingPoint(
                                customer,
                                usagePoint,
                                codingScheme,
                                agreement,
                                customerAccountAddress,
                                locationAddress
                        ));
                    }
                }
            }
        }
        return accountingPoints;
    }

    private AccountingPointComplexType createAccountingPoint(
            Customer customer,
            UsagePoint usagePoint,
            CodingSchemeTypeList codingScheme,
            @Nullable CustomerAgreement agreement,
            List<AddressComplexType> customerAccountAddress,
            List<AddressComplexType> locationAddress
    ) {
        var address = addressOfOrganisation(customer.getOrganisation());
        return new AccountingPointComplexType()
                .withMRID(
                        new MeasurementPointIDStringComplexType()
                                .withCodingScheme(codingScheme)
                                .withValue(getLastPathSegment(usagePoint))
                )
                .withContractPartyList(createContractPartyList(customer))
                .withBillingData(createBillingData(agreement))
                .withAddressList(
                        new AccountingPointComplexType.AddressList()
                                .withAddresses(toAddress(address))
                                .withAddresses(customerAccountAddress)
                                .withAddresses(locationAddress)
                );
    }

    private static List<UsagePoint> flattenUsagePoints(@Nullable ServiceLocation serviceLocation) {
        if (serviceLocation == null) return List.of();
        return serviceLocation.getUsagePoints()
                              .stream()
                              .map(UsagePoints::getUsagePoint)
                              .flatMap(List::stream)
                              .toList();
    }

    @Nullable
    private static BillingDataComplexType createBillingData(@Nullable CustomerAgreement agreement) {
        if (agreement == null) {
            return null;
        }
        return new BillingDataComplexType()
                .withReferenceNumber(agreement.getAgreementId());
    }

    private static AccountingPointComplexType.ContractPartyList createContractPartyList(Customer customer) {
        return new AccountingPointComplexType.ContractPartyList()
                .withContractParties(
                        new ContractPartyComplexType()
                                .withContractPartyRole(ContractPartyRoleType.CONTRACTPARTNER)
                                .withSurName(customer.getCustomerName())
                                .withIdentification(customer.getPucNumber())
                );
    }

    private static List<AddressComplexType> toAddress(@Nullable StreetAddress address) {
        if (address == null) {
            return List.of();
        }
        var streetDetail = address.getStreetDetail();
        var townDetail = address.getTownDetail();
        if (areAllNull(streetDetail.getName(),
                       streetDetail.getBuildingName(),
                       streetDetail.getNumber(),
                       streetDetail.getSuiteNumber())) {
            return List.of(
                    new AddressComplexType()
                            .withAddressSuffix(streetDetail.getAddressGeneral())
                            .withCityName(townDetail.getName())
                            .withPostalCode(address.getPostalCode())
            );
        }
        var add = new AddressComplexType()
                .withAddressRole(AddressRoleType.DELIVERY)
                .withStreetName(streetDetail.getName())
                .withCityName(townDetail.getName())
                .withBuildingNumber(streetDetail.getBuildingName())
                .withPostalCode(address.getPostalCode())
                .withDoorNumber(streetDetail.getNumber())
                .withFloorNumber(streetDetail.getSuiteNumber())
                .withAddressSuffix(streetDetail.getSuffix());
        return List.of(add);
    }

    private static String getLastPathSegment(UsagePoint usagePoint) {
        return new File(URI.create(usagePoint.getValue().trim()).getPath()).getName();
    }

    @Nullable
    private StreetAddress addressOfOrganisation(Organisation organisation) {
        var streetAddress = organisation.getStreetAddress();
        return streetAddress == null ? organisation.getPostalAddress() : streetAddress;
    }

    private static boolean areAllNull(java.lang.Object... objs) {
        return Arrays.stream(objs)
                     .allMatch(Objects::isNull);
    }

    private static <T> List<AddressComplexType> addressOfNullable(
            @Nullable T obj,
            Function<T, StreetAddress> accessor
    ) {
        return obj == null ? List.of() : toAddress(accessor.apply(obj));
    }
}
