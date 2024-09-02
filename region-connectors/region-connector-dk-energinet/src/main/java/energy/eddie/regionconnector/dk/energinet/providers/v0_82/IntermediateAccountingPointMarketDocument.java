package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.dk.energinet.customer.model.ContactAddressDto;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDto;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import org.apache.logging.log4j.util.Strings;

import java.time.ZonedDateTime;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.ValidatedHistoricalDataMarketDocumentBuilder.GLOBAL_LOCATION_NUMBER;

public record IntermediateAccountingPointMarketDocument(
        IdentifiableAccountingPointDetails identifiableAccountingPointDetails,
        CommonInformationModelConfiguration cimConfig
) {

    public static final PartyIDStringComplexType ENERGINET_MRID = new PartyIDStringComplexType()
            .withCodingScheme(CodingSchemeTypeList.GS1)
            .withValue(GLOBAL_LOCATION_NUMBER);

    // TODO update mapping with GH-1037
    public AccountingPointEnvelope accountingPointMarketDocument() {
        var meteringPointDetails = identifiableAccountingPointDetails.meteringPointDetails();
        return new APEnvelope(new AccountingPointMarketDocumentComplexType()
                                      .withMRID(UUID.randomUUID().toString())
                                      .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                                      .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                                      .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                                      .withSenderMarketParticipantMRID(ENERGINET_MRID)
                                      .withReceiverMarketParticipantMRID(receiverMRID())
                                      .withReceiverMarketParticipantMarketRoleType(RoleTypeList.PARTY_CONNECTED_TO_GRID)
                                      .withCreatedDateTime(createdDateTime())
                                      .withAccountingPointList(new AccountingPointMarketDocumentComplexType.AccountingPointList().withAccountingPoints(
                                              accountingPointComplexType(meteringPointDetails)
                                      )),
                              identifiableAccountingPointDetails.permissionRequest())
                .wrap();
    }

    private PartyIDStringComplexType receiverMRID() {
        return new PartyIDStringComplexType()
                .withCodingScheme(CodingSchemeTypeList.fromValue(
                        cimConfig.eligiblePartyNationalCodingScheme().value())
                )
                .withValue(cimConfig.eligiblePartyFallbackId());
    }

    private static String createdDateTime() {
        return new EsmpDateTime(ZonedDateTime.now(DK_ZONE_ID)).toString();
    }

    private AccountingPointComplexType accountingPointComplexType(MeteringPointDetailsCustomerDto meteringPointDetails) {
        var addresslist = new AccountingPointComplexType
                .AddressList().withAddresses(installationAddress(meteringPointDetails));

        var contractList = new AccountingPointComplexType.ContractPartyList();
        var contactAddresses = meteringPointDetails.getContactAddresses();
        if (contactAddresses != null) {
            for (ContactAddressDto contactAddress : contactAddresses) {
                contractList.withContractParties(contractParty(contactAddress));
                addresslist.withAddresses(contactAddresses(contactAddress));
            }
        }

        return new AccountingPointComplexType()
                .withSettlementMethod(meteringPointDetails.getSettlementMethod())
                .withMRID(new MeasurementPointIDStringComplexType()
                                  .withValue(meteringPointDetails.getMeteringPointId())
                                  .withCodingScheme(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME)
                )
                .withMeterReadingResolution(meteringPointDetails.getMeterReadingOccurrence())
                .withResolution(meteringPointDetails.getMeterReadingOccurrence())
                .withDirection(directionTypeList(meteringPointDetails))
                .withCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                .withSupplyStatus(meteringPointDetails.getPhysicalStatusOfMP())
                .withAddressList(addresslist)
                .withContractPartyList(contractList);
    }

    private static AddressComplexType installationAddress(MeteringPointDetailsCustomerDto meteringPointDetails) {
        return new AddressComplexType()
                .withAddressRole(AddressRoleType.DELIVERY)
                .withBuildingNumber(meteringPointDetails.getBuildingNumber())
                .withCityName(meteringPointDetails.getCityName())
                .withFloorNumber(meteringPointDetails.getFloorId())
                .withPostalCode(meteringPointDetails.getPostcode())
                .withDoorNumber(meteringPointDetails.getBuildingNumber())
                .withStreetName(meteringPointDetails.getStreetName());
    }

    private ContractPartyComplexType contractParty(ContactAddressDto contactAddress) {
        return new ContractPartyComplexType()
                .withContractPartyRole(ContractPartyRoleType.CONTRACTPARTNER)
                .withEmail(contactAddress.getContactEmailAddress())
                .withFirstName(contactAddress.getContactName1());
    }

    private AddressComplexType contactAddresses(ContactAddressDto contactAddress) {
        return new AddressComplexType()
                .withBuildingNumber(contactAddress.getBuildingNumber())
                .withCityName(contactAddress.getCityName())
                .withFloorNumber(contactAddress.getFloorId())
                .withPostalCode(contactAddress.getPostcode())
                .withDoorNumber(contactAddress.getBuildingNumber())
                .withStreetName(contactAddress.getStreetName())
                .withAddressSuffix(contactAddress.getAddressCode());
    }

    private static DirectionTypeList directionTypeList(MeteringPointDetailsCustomerDto meteringPointDetails) {
        return Strings.isNotBlank(meteringPointDetails.getMpCapacity())
                ? DirectionTypeList.UP
                : DirectionTypeList.DOWN;
    }
}
