package energy.eddie.regionconnector.de.eta.providers.cim.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusAccountingPointData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Intermediate representation for converting ETA Plus accounting point data to
 * CIM v0.82 format.
 * 
 * Aligns with EDDIE documentation:
 * -
 * https://architecture.eddie.energy/framework/3-extending/region-connector/quickstart.html#accounting-point-data
 * - Maps accounting point data to AccountingPointEnvelope for CIM v0.82
 * compatibility
 */
public final class IntermediateAccountingPointMarketDocument {

    private final DePermissionRequest permissionRequest;
    private final EtaPlusAccountingPointData accountingPointData;
    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration config;

    public IntermediateAccountingPointMarketDocument(
            IdentifiableAccountingPointData identifiableAccountingPointData,
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration config) {
        this.permissionRequest = identifiableAccountingPointData.permissionRequest();
        this.accountingPointData = identifiableAccountingPointData.payload();
        this.cimConfig = cimConfig;
        this.config = config;
    }

    /**
     * Creates the AccountingPointEnvelope for CIM v0.82.
     * 
     * @return the accounting point envelope
     */
    public AccountingPointEnvelope accountingPointEnvelope() {
        return new APEnvelope(
                accountingPointMarketDocument(),
                permissionRequest).wrap();
    }

    private AccountingPointMarketDocumentComplexType accountingPointMarketDocument() {
        return new AccountingPointMarketDocumentComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withSenderMarketParticipantMRID(senderMRID())
                .withReceiverMarketParticipantMRID(receiverMRID())
                .withCreatedDateTime(
                        new EsmpDateTime(ZonedDateTime.now(EtaRegionConnectorMetadata.DE_ZONE_ID)).toString())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                .withAccountingPointList(new AccountingPointMarketDocumentComplexType.AccountingPointList()
                        .withAccountingPoints(accountingPointComplexType()));
    }

    private PartyIDStringComplexType senderMRID() {
        // ETA Plus as the MDA
        return new PartyIDStringComplexType()
                .withCodingScheme(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME)
                .withValue(config.mdaIdentifier());
    }

    private PartyIDStringComplexType receiverMRID() {
        return new PartyIDStringComplexType()
                .withCodingScheme(CodingSchemeTypeList.fromValue(cimConfig.eligiblePartyNationalCodingScheme().value()))
                .withValue(config.eligiblePartyId());
    }

    private AccountingPointComplexType accountingPointComplexType() {
        var accountingPoint = new AccountingPointComplexType()
                .withMRID(new MeasurementPointIDStringComplexType()
                        .withValue(accountingPointData.meteringPointId())
                        .withCodingScheme(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME))
                .withCommodity(determineCommodity())
                .withAddressList(addressList());

        // Add optional fields if available
        if (accountingPointData.voltageLevel() != null) {
            // Map voltage level if needed
        }

        return accountingPoint;
    }

    private CommodityKind determineCommodity() {
        // Map energy type to CIM commodity kind
        if (accountingPointData.energyType() != null) {
            String energyType = accountingPointData.energyType().toUpperCase();
            if (energyType.contains("ELECTRICITY") || energyType.contains("STROM")) {
                return CommodityKind.ELECTRICITYPRIMARYMETERED;
            } else if (energyType.contains("GAS")) {
                return CommodityKind.NATURALGAS;
            }
        }
        // Default to electricity
        return CommodityKind.ELECTRICITYPRIMARYMETERED;
    }

    private AccountingPointComplexType.AddressList addressList() {
        var addressComplexType = new AddressComplexType()
                .withAddressRole(AddressRoleType.DELIVERY);

        // Use streetName() method which falls back to address if streetName is null
        String streetName = accountingPointData.streetName();
        if (streetName != null && !streetName.isBlank()) {
            addressComplexType.withStreetName(streetName);
        }
        if (accountingPointData.postalCode() != null) {
            addressComplexType.withPostalCode(accountingPointData.postalCode());
        }
        if (accountingPointData.city() != null) {
            addressComplexType.withCityName(accountingPointData.city());
        }
        // Note: CIM v0.82 AddressComplexType does not include a country field

        return new AccountingPointComplexType.AddressList()
                .withAddresses(addressComplexType);
    }
}
