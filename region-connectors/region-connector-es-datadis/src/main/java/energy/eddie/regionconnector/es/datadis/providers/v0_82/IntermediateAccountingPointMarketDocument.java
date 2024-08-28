package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.dtos.Address;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import org.apache.logging.log4j.util.Strings;

import java.time.ZonedDateTime;
import java.util.UUID;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;


public final class IntermediateAccountingPointMarketDocument {

    private final EsPermissionRequest permissionRequest;
    private final AccountingPointData accountingPointData;
    private final CommonInformationModelConfiguration cimConfig;
    private final DatadisConfig datadisConfig;

    public IntermediateAccountingPointMarketDocument(
            IdentifiableAccountingPointData identifiableAccountingPointData,
            CommonInformationModelConfiguration cimConfig,
            DatadisConfig datadisConfig
    ) {
        this.permissionRequest = identifiableAccountingPointData.permissionRequest();
        this.accountingPointData = identifiableAccountingPointData.accountingPointData();
        this.cimConfig = cimConfig;
        this.datadisConfig = datadisConfig;
    }

    public AccountingPointEnveloppe accountingPointEnveloppe() {
        return new APEnvelope(
                accountingPointMarketDocument(),
                permissionRequest
        )
                .wrap();
    }

    private AccountingPointMarketDocumentComplexType accountingPointMarketDocument() {
        return new AccountingPointMarketDocumentComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withSenderMarketParticipantMRID(senderMRID())
                .withReceiverMarketParticipantMRID(receiverMRID())
                .withCreatedDateTime(new EsmpDateTime(ZonedDateTime.now(ZONE_ID_SPAIN)).toString())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                .withAccountingPointList(new AccountingPointMarketDocumentComplexType.AccountingPointList()
                                                 .withAccountingPoints(accountingPointComplexType())
                );
    }

    private PartyIDStringComplexType senderMRID() {
        return new PartyIDStringComplexType()
                .withCodingScheme(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME)
                .withValue(permissionRequest
                                   .distributorCode()
                                   .map(DistributorCode::name)
                                   .orElse("Datadis"));
    }

    private PartyIDStringComplexType receiverMRID() {
        return new PartyIDStringComplexType()
                .withCodingScheme(CodingSchemeTypeList.fromValue(cimConfig.eligiblePartyNationalCodingScheme()
                                                                          .value()))
                .withValue(datadisConfig.username());
    }

    private AccountingPointComplexType accountingPointComplexType() {
        return new AccountingPointComplexType()
                .withDirection(direction())
                .withMRID(new MeasurementPointIDStringComplexType()
                                  .withValue(accountingPointData.contractDetails().cups())
                                  .withCodingScheme(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME))
                .withCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                .withMeterReadingResolution(resolution())
                .withTariffClassDSO(accountingPointData.contractDetails().codeFare())
                .withAddressList(addressList())
                .withBillingData(billingData());
    }

    private DirectionTypeList direction() {
        return accountingPointData
                .contractDetails()
                .installedCapacity()
                .map(Strings::isNotBlank)
                .filter(capacity -> capacity)
                .map(capacity -> DirectionTypeList.UP_AND_DOWN)
                .orElse(DirectionTypeList.DOWN);
    }

    private String resolution() {
        // Only point types 1 and 2 provide quarter hourly data
        return switch (accountingPointData.supply().pointType()) {
            case 1, 2 -> Granularity.PT15M.name();
            default -> Granularity.PT1H.name();
        };
    }

    private AccountingPointComplexType.AddressList addressList() {
        return new AccountingPointComplexType.AddressList()
                .withAddresses(
                        installationAddress()
                );
    }

    private BillingDataComplexType billingData() {
        return new BillingDataComplexType()
                .withGridAgreementTypeDescription(accountingPointData.contractDetails().accessFare());
    }

    private AddressComplexType installationAddress() {
        var addressLine = accountingPointData.supply().address();
        var addressComplexType = new AddressComplexType().withAddressRole(AddressRoleType.DELIVERY);

        var optionalParsedAddress = Address.parse(accountingPointData.supply().address());
        if (optionalParsedAddress.isPresent()) {
            var parsedAddress = optionalParsedAddress.get();
            addressComplexType.withStreetName(parsedAddress.street())
                              .withBuildingNumber(parsedAddress.buildingNumber())
                              .withDoorNumber(parsedAddress.door())
                              .withFloorNumber(parsedAddress.floor())
                              .withCityName(parsedAddress.city())
                              .withPostalCode(parsedAddress.postalCode())
                              .withAddressSuffix(parsedAddress.province());
        } else {
            addressComplexType.withAddressSuffix(addressLine);
        }
        return addressComplexType;
    }
}
