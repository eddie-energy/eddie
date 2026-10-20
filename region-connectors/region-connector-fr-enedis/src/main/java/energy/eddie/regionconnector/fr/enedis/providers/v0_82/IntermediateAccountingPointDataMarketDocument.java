// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.address.InstallationAddress;
import energy.eddie.regionconnector.fr.enedis.dto.situation.ContractualSituation;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;
import jakarta.annotation.Nullable;

import java.util.List;
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
                          .withValue(receiverMarketParticipantValue())
          )
          .withAccountingPointList(
                  new AccountingPointMarketDocumentComplexType.AccountingPointList()
                          .withAccountingPoints(accountingPoints())
          );
        FrEnedisPermissionRequest permissionRequest = identifiableAccountingPointData.permissionRequest();
        return new APEnvelope(ap, permissionRequest).wrap();
    }

    /**
     * The situation contractuelle response has no customer id, so the SIREN of an organization
     * customer identifies the receiver; natural person customers leave it empty.
     */
    private @Nullable String receiverMarketParticipantValue() {
        for (ContractualSituation situation : identifiableAccountingPointData.situations()) {
            if (situation.organization() != null) {
                return situation.organization().sirenNumber();
            }
        }
        return null;
    }

    private List<AccountingPointComplexType> accountingPoints() {
        return identifiableAccountingPointData.situations()
                                              .stream()
                                              .map(situation ->
                                                           new AccountingPointComplexType()
                                                                   .withCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                                                                   .withMRID(measurementPointIDStringComplexType(
                                                                           situation))
                                                                   .withContractPartyList(contractPartyList(situation))
                                                                   .withAddressList(addressList())
                                                                   .withTariffClassDSO(situation.distributionTariff())
                                                                   .withDirection(direction(situation))
                                              )
                                              .toList();
    }

    private static MeasurementPointIDStringComplexType measurementPointIDStringComplexType(ContractualSituation situation) {
        return new MeasurementPointIDStringComplexType()
                .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                .withValue(situation.usagePointId());
    }

    private AccountingPointComplexType.ContractPartyList contractPartyList(ContractualSituation situation) {
        var person = situation.person();
        var organization = situation.organization();
        var contactData = situation.contactData();
        var installationAddress = installationAddress();
        var contractParty = new ContractPartyComplexType()
                .withContractPartyRole(ContractPartyRoleType.CONTRACTPARTNER)
                .withSalutation(person != null ? person.title() : null)
                .withFirstName(person != null ? person.firstName() : null)
                .withSurName(person != null ? person.lastName() : null)
                .withCompanyName(organization != null ? organization.name() : null)
                .withVATnumber(organization != null ? organization.siretNumber() : null)
                .withEmail(contactData != null ? contactData.email() : null)
                .withIdentification(installationAddress != null ? installationAddress.inseeCode() : null);

        return new AccountingPointComplexType.ContractPartyList().withContractParties(contractParty);
    }

    private @Nullable InstallationAddress installationAddress() {
        var generalData = identifiableAccountingPointData.generalData();
        if (generalData == null || generalData.address() == null) {
            return null;
        }
        return generalData.address().address();
    }

    private AccountingPointComplexType.AddressList addressList() {
        var installationAddress = installationAddress();
        return new AccountingPointComplexType.AddressList().withAddresses(
                new AddressComplexType()
                        .withAddressRole(AddressRoleType.DELIVERY)
                        .withPostalCode(installationAddress != null ? installationAddress.postalCodeCity() : null)
                        .withStreetName(installationAddress != null ? installationAddress.numberStreetName() : null)
                        .withAddressSuffix(installationAddress != null ? installationAddress.locality() : null)
        );
    }

    private @Nullable DirectionTypeList direction(ContractualSituation situation) {
        var segments = situation.segments() == null ? List.<String>of() : situation.segments();
        var usagePointType = UsagePointType.fromSegments(segments);
        return usagePointType.map(pointType -> switch (pointType) {
            case CONSUMPTION -> DirectionTypeList.DOWN;
            case PRODUCTION -> DirectionTypeList.UP;
            case CONSUMPTION_AND_PRODUCTION -> DirectionTypeList.UP_AND_DOWN;
        }).orElse(null);
    }
}