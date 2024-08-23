package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.address.CustomerAddress;
import energy.eddie.regionconnector.fr.enedis.dto.contact.CustomerContact;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.identity.CustomerIdentity;
import energy.eddie.regionconnector.fr.enedis.dto.identity.LegalEntity;
import energy.eddie.regionconnector.fr.enedis.dto.identity.NaturalPerson;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisDataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateAccountingPointDataMarketDocumentTest {

    @ParameterizedTest
    @ValueSource(strings = {
            TestResourceProvider.IDENTITY,
            TestResourceProvider.IDENTITY_LEGAL_ONLY,
            TestResourceProvider.IDENTITY_NATURAL_ONLY
    })
    @SuppressWarnings("java:S5961")
        // suppress too many assertions warning
    void eddieAccountingPointMarketDocument(String identityResource) throws IOException {
        // Given
        var contract = TestResourceProvider.readFromFile(TestResourceProvider.CONTRACT, CustomerContract.class);
        var address = TestResourceProvider.readFromFile(TestResourceProvider.ADDRESS, CustomerAddress.class);
        var identity = TestResourceProvider.readFromFile(identityResource, CustomerIdentity.class);
        var contact = TestResourceProvider.readFromFile(TestResourceProvider.CONTACT, CustomerContact.class);
        var permissionRequest = permissionRequest();
        var identifiableAccountingPointData = new IdentifiableAccountingPointData(
                permissionRequest,
                contract,
                address,
                identity,
                contact
        );
        var intermediateAccountingPointDataMarketDocument = new IntermediateAccountingPointDataMarketDocument(
                identifiableAccountingPointData,
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                        "fallbackId"
                )
        );

        // When
        var res = intermediateAccountingPointDataMarketDocument.eddieAccountingPointMarketDocument();

        // Then
        var md = res.marketDocument();
        var ap = md.getAccountingPointList().getAccountingPoints().getFirst();
        var cp = ap.getContractPartyList().getContractParties().getFirst();
        var add = ap.getAddressList().getAddresses().getFirst();
        assertAll(
//region Meta Information
                () -> assertEquals(permissionRequest.permissionId(), res.permissionId()),
                () -> assertEquals(permissionRequest.connectionId(), res.connectionId()),
                () -> assertEquals(permissionRequest.dataNeedId(), res.dataNeedId()),
                () -> assertNotNull(md.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), md.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA, md.getType()),
                () -> assertNotNull(md.getCreatedDateTime()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   md.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER,
                                   md.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME,
                                   md.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("ENEDIS",
                                   md.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   md.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(contract.customerId(),
                                   md.getReceiverMarketParticipantMRID().getValue()),
//endregion
//region Accounting Point
                () -> assertEquals(1, md.getAccountingPointList().getAccountingPoints().size()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED, ap.getCommodity()),
                () -> assertEquals(DirectionTypeList.DOWN, ap.getDirection()),
                () -> assertEquals(contract.usagePointContracts().getFirst().contract().distributionTariff(),
                                   ap.getTariffClassDSO()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME,
                                   ap.getMRID().getCodingScheme()),
                () -> assertEquals(permissionRequest.usagePointId(), ap.getMRID().getValue()),
//endregion
//region Contract Party
                () -> assertEquals(1, ap.getContractPartyList().getContractParties().size()),
                () -> assertEquals(ContractPartyRoleType.CONTRACTPARTNER, cp.getContractPartyRole()),
                () -> assertEquals(identity.identity().naturalPerson().map(NaturalPerson::title).orElse(null),
                                   cp.getSalutation()),
                () -> assertEquals(identity.identity().naturalPerson().map(NaturalPerson::lastName).orElse(null),
                                   cp.getSurName()),
                () -> assertEquals(identity.identity().naturalPerson().map(NaturalPerson::firstName).orElse(null),
                                   cp.getFirstName()),
                () -> assertEquals(identity.identity().legalEntity().map(LegalEntity::name).orElse(null),
                                   cp.getCompanyName()),
                () -> assertEquals(address.usagePoints().getFirst().address().inseeCode(), cp.getIdentification()),
                () -> assertEquals(contact.contact().email(), cp.getEmail()),
                () -> assertEquals(identity.identity().legalEntity().map(LegalEntity::siretNumber).orElse(null),
                                   cp.getVATnumber()),
//endregion
//region Address
                () -> assertEquals(1, ap.getAddressList().getAddresses().size()),
                () -> assertEquals(AddressRoleType.DELIVERY, add.getAddressRole()),
                () -> assertEquals(address.usagePoints().getFirst().address().postalCode(), add.getPostalCode()),
                () -> assertEquals(address.usagePoints().getFirst().address().city(), add.getCityName()),
                () -> assertEquals(address.usagePoints().getFirst().address().street(), add.getStreetName()),
                () -> assertEquals(address.usagePoints().getFirst().address().locality(), add.getAddressSuffix())
//endregion
        );
    }

    private FrEnedisPermissionRequest permissionRequest() {
        return new SimpleFrEnedisPermissionRequest(
                "usagePointId",
                null,
                UsagePointType.CONSUMPTION,
                Optional.empty(),
                "permissionId",
                "connectionId",
                "dataNeedId",
                PermissionProcessStatus.ACCEPTED,
                new EnedisDataSourceInformation(),
                ZonedDateTime.now(),
                LocalDate.now(),
                LocalDate.now()
        );
    }
}
