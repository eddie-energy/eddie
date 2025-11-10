package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoint;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.ap.APEnvelope;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class IntermediateAccountingPointDataMarketDocument {
    private final MijnAansluitingPermissionRequest permissionRequest;
    private final List<MeteringPoint> meteringPoints;
    private final MijnAansluitingConfiguration config;

    IntermediateAccountingPointDataMarketDocument(
            IdentifiableAccountingPointData data,
            MijnAansluitingConfiguration config
    ) {
        this.permissionRequest = data.permissionRequest();
        this.meteringPoints = data.payload();
        this.config = config;
    }

    List<AccountingPointEnvelope> toAp() {
        var aps = new ArrayList<AccountingPointEnvelope>();
        for (MeteringPoint meteringPoint : meteringPoints) {
            var ap = createAccountingPointMarketDocument(meteringPoint);
            var envelope = new APEnvelope(ap, permissionRequest).wrap();
            aps.add(envelope);
        }
        return aps;
    }

    private AccountingPointMarketDocumentComplexType createAccountingPointMarketDocument(MeteringPoint meteringPoint) {
        var now = EsmpDateTime.now();
        return new AccountingPointMarketDocumentComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA)
                .withCreatedDateTime(now.toString())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.PARTY_CONNECTED_TO_GRID)
                .withSenderMarketParticipantMRID(getParticipantMRID(config.continuousClientId().getValue()))
                .withReceiverMarketParticipantMRID(getParticipantMRID(meteringPoint.getGridOperatorEan()))
                .withAccountingPointList(
                        new AccountingPointMarketDocumentComplexType.AccountingPointList()
                                .withAccountingPoints(
                                        createAccountingPoint(meteringPoint)
                                )
                );
    }

    private PartyIDStringComplexType getParticipantMRID(String config) {
        return new PartyIDStringComplexType()
                .withCodingScheme(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME)
                .withValue(config);
    }

    private static AccountingPointComplexType createAccountingPoint(MeteringPoint meteringPoint) {
        var address = getAddress(meteringPoint);
        return new AccountingPointComplexType()
                .withCommodity(getCommodity(meteringPoint.getProduct()))
                .withMRID(
                        new MeasurementPointIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME)
                                .withValue(meteringPoint.getEan())
                )
                .withAddressList(
                        new AccountingPointComplexType.AddressList()
                                .withAddresses(address == null ? List.of() : List.of(address))
                );
    }

    @Nullable
    private static AddressComplexType getAddress(MeteringPoint meteringPoint) {
        var address = meteringPoint.getAddress();
        if (address == null) {
            return null;
        }
        var streetNumber = address.getStreetNumber() == null ? null : address.getStreetNumber().toString();
        return new AddressComplexType()
                .withAddressRole(AddressRoleType.DELIVERY)
                .withStreetName(address.getStreet())
                .withCityName(address.getCity())
                .withPostalCode(address.getPostalCode())
                .withDoorNumber(streetNumber)
                .withAddressSuffix(address.getStreetNumberAddition());
    }

    @Nullable
    private static CommodityKind getCommodity(@Nullable MeteringPoint.ProductEnum product) {
        return switch (product) {
            case ELK -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case GAS -> CommodityKind.NATURALGAS;
            case null, default -> null;
        };
    }
}
