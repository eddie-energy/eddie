// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.AccountingPointComplexType;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.ap.AccountingPointMarketDocumentComplexType;
import energy.eddie.cim.v0_82.ap.AddressComplexType;
import energy.eddie.cim.v0_82.ap.AddressRoleType;
import energy.eddie.cim.v0_82.ap.CodingSchemeTypeList;
import energy.eddie.cim.v0_82.ap.CommodityKind;
import energy.eddie.cim.v0_82.ap.ContractPartyComplexType;
import energy.eddie.cim.v0_82.ap.ContractPartyRoleType;
import energy.eddie.cim.v0_82.ap.DirectionTypeList;
import energy.eddie.cim.v0_82.ap.MeasurementPointIDStringComplexType;
import energy.eddie.cim.v0_82.ap.MessageTypeList;
import energy.eddie.cim.v0_82.ap.PartyIDStringComplexType;
import energy.eddie.cim.v0_82.ap.RoleTypeList;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusAccountingPointData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Maps a single ETA Plus accounting-point response to a CIM v0.82 {@link AccountingPointEnvelope}.
 *
 * <p>One MP → one envelope → one {@link AccountingPointComplexType}.
 */
class IntermediateAccountingPointDataMarketDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateAccountingPointDataMarketDocument.class);

    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration deConfiguration;
    private final IdentifiableAccountingPointData identifiableData;

    IntermediateAccountingPointDataMarketDocument(
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration,
            IdentifiableAccountingPointData identifiableData
    ) {
        this.cimConfig = cimConfig;
        this.deConfiguration = deConfiguration;
        this.identifiableData = identifiableData;
    }

    public AccountingPointEnvelope toEnvelope() {
        var permissionRequest = identifiableData.permissionRequest();
        var payload = identifiableData.payload();
        var senderId = permissionRequest.dataSourceInformation().meteredDataAdministratorId();

        var ap = new AccountingPointMarketDocumentComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                .withCreatedDateTime(new EsmpDateTime(ZonedDateTime.now(ZoneOffset.UTC)).toString())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME)
                                .withValue(senderId)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.fromValue(
                                        cimConfig.eligiblePartyNationalCodingScheme().value()))
                                .withValue(deConfiguration.eligiblePartyId())
                )
                .withAccountingPointList(
                        new AccountingPointMarketDocumentComplexType.AccountingPointList()
                                .withAccountingPoints(accountingPoint(payload))
                );

        return new APEnvelope(ap, permissionRequest).wrap();
    }

    private AccountingPointComplexType accountingPoint(EtaPlusAccountingPointData payload) {
        var point = new AccountingPointComplexType()
                .withMRID(new MeasurementPointIDStringComplexType()
                        .withCodingScheme(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME)
                        .withValue(payload.meteringPointId()))
                .withContractPartyList(contractPartyList(payload.contractParty()))
                .withAddressList(addressList(payload.deliveryAddress()));

        var commodity = commodityFor(payload.energyType());
        if (commodity != null) {
            point.withCommodity(commodity);
        }
        var direction = directionFor(payload.direction());
        if (direction != null) {
            point.withDirection(direction);
        }
        return point;
    }

    private AccountingPointComplexType.ContractPartyList contractPartyList(@Nullable EtaPlusAccountingPointData.ContractParty contractParty) {
        var list = new AccountingPointComplexType.ContractPartyList();
        if (contractParty == null) {
            return list;
        }
        var party = new ContractPartyComplexType()
                .withContractPartyRole(ContractPartyRoleType.CONTRACTPARTNER);
        if (contractParty.firstName() != null) {
            party.setFirstName(contractParty.firstName());
        }
        if (contractParty.surName() != null) {
            party.setSurName(contractParty.surName());
        }
        if (contractParty.companyName() != null) {
            party.setCompanyName(contractParty.companyName());
        }
        if (contractParty.email() != null) {
            party.setEmail(contractParty.email());
        }
        return list.withContractParties(party);
    }

    private AccountingPointComplexType.AddressList addressList(@Nullable EtaPlusAccountingPointData.Address address) {
        var list = new AccountingPointComplexType.AddressList();
        if (address == null) {
            return list;
        }
        var entry = new AddressComplexType()
                .withAddressRole(AddressRoleType.DELIVERY);
        if (address.streetName() != null) {
            String fullStreet = address.buildingNumber() != null
                    ? address.streetName() + " " + address.buildingNumber()
                    : address.streetName();
            entry.setStreetName(fullStreet);
        }
        if (address.postalCode() != null) {
            entry.setPostalCode(address.postalCode());
        }
        if (address.city() != null) {
            entry.setCityName(address.city());
        }
        if (address.addressSuffix() != null) {
            entry.setAddressSuffix(address.addressSuffix());
        }
        return list.withAddresses(entry);
    }

    @Nullable
    private CommodityKind commodityFor(@Nullable String energyType) {
        if (energyType == null) {
            return null;
        }
        return switch (energyType) {
            case "ELECTRICITY" -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case "NATURAL_GAS" -> CommodityKind.NATURALGAS;
            default -> {
                LOGGER.warn("Unsupported energyType '{}' on metering point {}; omitting commodity",
                        energyType, identifiableData.payload().meteringPointId());
                yield null;
            }
        };
    }

    @Nullable
    private DirectionTypeList directionFor(@Nullable String direction) {
        if (direction == null) {
            return null;
        }
        return switch (direction) {
            case "Consumption" -> DirectionTypeList.DOWN;
            case "Generation", "Production" -> DirectionTypeList.UP;
            case "ConsumptionAndGeneration", "ConsumptionAndProduction" -> DirectionTypeList.UP_AND_DOWN;
            default -> {
                LOGGER.warn("Unknown direction '{}' on metering point {}; omitting direction",
                        direction, identifiableData.payload().meteringPointId());
                yield null;
            }
        };
    }
}