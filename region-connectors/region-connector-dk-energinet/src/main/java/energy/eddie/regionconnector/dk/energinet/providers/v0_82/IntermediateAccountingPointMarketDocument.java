package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.customer.model.ContactAddressDto;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDto;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;
import org.apache.logging.log4j.util.Strings;

import java.time.ZonedDateTime;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;

public final class IntermediateAccountingPointMarketDocument {

    private final PartyIDStringComplexType energinetMRID = new PartyIDStringComplexType()
            .withCodingScheme(CodingSchemeTypeList.GS1)
            .withValue(EnerginetRegionConnectorMetadata.GLOBAL_LOCATION_NUMBER);
    private final IdentifiableAccountingPointDetails identifiableAccountingPointDetails;
    private final CommonInformationModelConfiguration cimConfig;

    public IntermediateAccountingPointMarketDocument(
            IdentifiableAccountingPointDetails identifiableAccountingPointDetails,
            CommonInformationModelConfiguration cimConfig
    ) {
        this.identifiableAccountingPointDetails = identifiableAccountingPointDetails;
        this.cimConfig = cimConfig;
    }

    public AccountingPointEnvelope accountingPointMarketDocument() {
        var meteringPointDetails = identifiableAccountingPointDetails.meteringPointDetails();
        var accountingPoint = accountingPointComplexType(meteringPointDetails);
        return new APEnvelope(new AccountingPointMarketDocumentComplexType()
                                      .withMRID(UUID.randomUUID().toString())
                                      .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                                      .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                                      .withCreatedDateTime(createdDateTime())
                                      .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                                      .withSenderMarketParticipantMRID(energinetMRID)
                                      .withReceiverMarketParticipantMRID(receiverMRID())
                                      .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                                      .withAccountingPointList(new AccountingPointMarketDocumentComplexType.AccountingPointList()
                                                                       .withAccountingPoints(accountingPoint)),
                              identifiableAccountingPointDetails.permissionRequest())
                .wrap();
    }

    private static String createdDateTime() {
        return new EsmpDateTime(ZonedDateTime.now(DK_ZONE_ID)).toString();
    }

    private PartyIDStringComplexType receiverMRID() {
        return new PartyIDStringComplexType()
                .withCodingScheme(CodingSchemeTypeList.fromValue(
                        cimConfig.eligiblePartyNationalCodingScheme().value())
                )
                .withValue(cimConfig.eligiblePartyFallbackId());
    }

    private AccountingPointComplexType accountingPointComplexType(MeteringPointDetailsCustomerDto meteringPointDetails) {
        var addresslist = new AccountingPointComplexType.AddressList()
                .withAddresses(installationAddress(meteringPointDetails));

        var contractList = new AccountingPointComplexType.ContractPartyList();
        var contactAddresses = meteringPointDetails.getContactAddresses();
        if (contactAddresses != null) {
            for (ContactAddressDto contactAddress : contactAddresses) {
                contractList.withContractParties(contractParty(meteringPointDetails, contactAddress));
                addresslist.withAddresses(contactAddresses(contactAddress));
            }
        }

        return new AccountingPointComplexType()
                .withSettlementMethod(meteringPointDetails.getSettlementMethod())
                .withMeterReadingResolution(meteringPointDetails.getMeterReadingOccurrence())
                .withResolution(meteringPointDetails.getMeterReadingOccurrence())
                .withDirection(directionTypeList(meteringPointDetails))
                .withCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                .withSupplyStatus(meteringPointDetails.getPhysicalStatusOfMP())
                .withMRID(new MeasurementPointIDStringComplexType()
                                  .withValue(meteringPointDetails.getMeteringPointId())
                                  .withCodingScheme(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME)
                )
                .withAddressList(addresslist)
                .withContractPartyList(contractList);
    }

    private static AddressComplexType installationAddress(MeteringPointDetailsCustomerDto meteringPointDetails) {
        var street = meteringPointDetails.getStreetCode() == null || meteringPointDetails.getStreetCode().isBlank()
                ? meteringPointDetails.getStreetName()
                : meteringPointDetails.getStreetCode() + " " + meteringPointDetails.getStreetName();
        return new AddressComplexType()
                .withAddressRole(AddressRoleType.DELIVERY)
                .withBuildingNumber(meteringPointDetails.getBuildingNumber())
                .withCityName(meteringPointDetails.getCityName())
                .withFloorNumber(meteringPointDetails.getFloorId())
                .withPostalCode(meteringPointDetails.getPostcode())
                .withDoorNumber(meteringPointDetails.getRoomId())
                .withStreetName(street);
    }

    private ContractPartyComplexType contractParty(
            MeteringPointDetailsCustomerDto meteringPointDetailsCustomerDto,
            ContactAddressDto contactAddress
    ) {
        return new ContractPartyComplexType()
                .withContractPartyRole(ContractPartyRoleType.CONTRACTPARTNER)
                .withSurName(meteringPointDetailsCustomerDto.getFirstConsumerPartyName())
                .withEmail(contactAddress.getContactEmailAddress());
    }

    private AddressComplexType contactAddresses(ContactAddressDto contactAddress) {
        return new AddressComplexType()
                .withAddressRole(AddressRoleType.INVOICE)
                .withBuildingNumber(contactAddress.getBuildingNumber())
                .withCityName(contactAddress.getCityName())
                .withFloorNumber(contactAddress.getFloorId())
                .withPostalCode(contactAddress.getPostcode())
                .withDoorNumber(contactAddress.getRoomId())
                .withStreetName(contactAddress.getStreetName())
                .withAddressSuffix(contactAddress.getAddressCode());
    }

    private static DirectionTypeList directionTypeList(MeteringPointDetailsCustomerDto meteringPointDetails) {
        return Strings.isNotBlank(meteringPointDetails.getMpCapacity())
                ? DirectionTypeList.UP
                : DirectionTypeList.DOWN;
    }
}
