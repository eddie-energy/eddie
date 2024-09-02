package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.address.Address;
import energy.eddie.regionconnector.fr.enedis.dto.identity.LegalEntity;
import energy.eddie.regionconnector.fr.enedis.dto.identity.NaturalPerson;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import jakarta.annotation.Nullable;

import java.util.UUID;

public final class IntermediateAccountingPointDataMarketDocument {
    private final AccountingPointMarketDocumentComplexType ap = new AccountingPointMarketDocumentComplexType()
            .withMRID(UUID.randomUUID().toString())
            .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
            .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
            .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
            .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
            .withSenderMarketParticipantMRID(
                    new PartyIDStringComplexType()
                            .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                            .withValue("ENEDIS") // No Mapping
            );
    private final CommonInformationModelConfiguration cimConfig;
    private final IdentifiableAccountingPointData identifiableAccountingPointData;

    IntermediateAccountingPointDataMarketDocument(
            IdentifiableAccountingPointData identifiableAccountingPointData,
            CommonInformationModelConfiguration cimConfig
    ) {
        this.identifiableAccountingPointData = identifiableAccountingPointData;
        this.cimConfig = cimConfig;
    }

    public AccountingPointEnvelope accountingPointEnvelope() {
        ap.withCreatedDateTime(EsmpDateTime.now().toString())
          .withReceiverMarketParticipantMRID(
                  new PartyIDStringComplexType()
                          .withCodingScheme(CodingSchemeTypeList.fromValue(
                                  cimConfig.eligiblePartyNationalCodingScheme().value()
                          ))
                          .withValue(identifiableAccountingPointData.address().customerId())
          )
          .withAccountingPointList(
                  new AccountingPointMarketDocumentComplexType.AccountingPointList()
                          .withAccountingPoints(accountingPoint())
          );
        FrEnedisPermissionRequest permissionRequest = identifiableAccountingPointData.permissionRequest();
        return new APEnvelope(ap, permissionRequest).wrap();
    }


    private AccountingPointComplexType accountingPoint() {
        return new AccountingPointComplexType()
                .withCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                .withMRID(measurementPointIDStringComplexType())
                .withContractPartyList(contractPartyList())
                .withAddressList(addressList())
                .withTariffClassDSO(distributionTariff())
                .withDirection(direction());
    }

    private MeasurementPointIDStringComplexType measurementPointIDStringComplexType() {
        return new MeasurementPointIDStringComplexType()
                .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                .withValue(identifiableAccountingPointData.permissionRequest().usagePointId());
    }

    private AccountingPointComplexType.ContractPartyList contractPartyList() {
        var identity = identifiableAccountingPointData.identity().identity();
        var contact = identifiableAccountingPointData.contact().contact();
        Address address = identifiableAccountingPointData.address().usagePoints().getFirst().address();
        var contractParty = new ContractPartyComplexType()
                .withContractPartyRole(ContractPartyRoleType.CONTRACTPARTNER)
                .withSalutation(identity.naturalPerson().map(NaturalPerson::title).orElse(null))
                .withFirstName(identity.naturalPerson().map(NaturalPerson::firstName).orElse(null))
                .withSurName(identity.naturalPerson().map(NaturalPerson::lastName).orElse(null))
                .withCompanyName(identity.legalEntity().map(LegalEntity::name).orElse(null))
                .withVATnumber(identity.legalEntity().map(LegalEntity::siretNumber).orElse(null))
                .withEmail(contact.email())
                .withIdentification(address != null ? address.inseeCode() : null);

        return new AccountingPointComplexType.ContractPartyList().withContractParties(contractParty);
    }

    private AccountingPointComplexType.AddressList addressList() {
        var address = identifiableAccountingPointData.address().usagePoints().getFirst().address();
        return new AccountingPointComplexType.AddressList().withAddresses(
                new AddressComplexType()
                        .withAddressRole(AddressRoleType.DELIVERY)
                        .withPostalCode(address.postalCode())
                        .withCityName(address.city())
                        .withStreetName(address.street())
                        .withAddressSuffix(address.locality())
        );
    }

    private String distributionTariff() {
        return identifiableAccountingPointData.contract()
                                              .usagePointContracts()
                                              .getFirst()
                                              .contract()
                                              .distributionTariff();
    }

    private @Nullable DirectionTypeList direction() {
        var usagePointType = UsagePointType.fromCustomerContract(identifiableAccountingPointData.contract());
        return usagePointType.map(pointType -> switch (pointType) {
            case CONSUMPTION -> DirectionTypeList.DOWN;
            case PRODUCTION -> DirectionTypeList.UP;
            case CONSUMPTION_AND_PRODUCTION -> DirectionTypeList.UP_AND_DOWN;
        }).orElse(null);
    }
}
