// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.dto.address.AddressData;
import energy.eddie.regionconnector.fr.enedis.dto.address.InstallationAddress;
import energy.eddie.regionconnector.fr.enedis.dto.address.UsagePointGeneralData;
import energy.eddie.regionconnector.fr.enedis.dto.situation.*;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisDataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateAccountingPointDataMarketDocumentTest {

    @Test
    @SuppressWarnings("java:S5961")
        // suppress too many assertions warning
    void accountingPointEnvelope_withNaturalPerson() {
        // Given: the shape Enedis actually returns (contact_data and person are element-root siblings)
        var situation = new ContractualSituation(
                "3127069600", "2021-10-23T00:00:00+0200", "Contrat Protocole501", "ELECTRICITE DE FRANCE",
                null, null, "Tarif BT<=36kVA", null, null, null, null, "6", List.of("C5"),
                new Customer(new CustomerAddress(new Address("M VIGNAL ANDRE", null, null, "rue DU CENTRE", null,
                                                             "34210 AIGUES VIVES", null))),
                new ContactData(null, "0000000000", null),
                new Person("M", "VIGNAL", "ANDRE"),
                null);
        var generalData = new UsagePointGeneralData(
                new AddressData(new InstallationAddress(
                        "Esc A, étage 2, apt 12", "Bâtiment A", "12 rue de la Paix", "Les Prés",
                        "75000", "75112")));
        var permissionRequest = permissionRequest();
        var identifiableAccountingPointData = new IdentifiableAccountingPointData(
                permissionRequest,
                List.of(situation),
                generalData
        );
        var intermediateAccountingPointDataMarketDocument = new IntermediateAccountingPointDataMarketDocument(
                identifiableAccountingPointData,
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                        "fallbackId"
                )
        );

        // When
        var res = intermediateAccountingPointDataMarketDocument.accountingPointEnvelope();

        // Then
        var md = res.getAccountingPointMarketDocument();
        var header = res.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        var ap = md.getAccountingPointList().getAccountingPoints().getFirst();
        var cp = ap.getContractPartyList().getContractParties().getFirst();
        var add = ap.getAddressList().getAddresses().getFirst();
        assertAll(
//region Meta Information
                () -> assertEquals(permissionRequest.permissionId(), header.getPermissionid()),
                () -> assertEquals(permissionRequest.connectionId(), header.getConnectionid()),
                () -> assertEquals(permissionRequest.dataNeedId(), header.getDataNeedid()),
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
                () -> assertNull(md.getReceiverMarketParticipantMRID().getValue()),
//endregion
//region Accounting Point
                () -> assertEquals(1, md.getAccountingPointList().getAccountingPoints().size()),
                () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED, ap.getCommodity()),
                () -> assertEquals(DirectionTypeList.DOWN, ap.getDirection()),
                () -> assertEquals("Tarif BT<=36kVA", ap.getTariffClassDSO()),
                () -> assertEquals(energy.eddie.cim.v0_82.ap.CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME,
                                   ap.getMRID().getCodingScheme()),
                () -> assertEquals("3127069600", ap.getMRID().getValue()),
//endregion
//region Contract Party
                () -> assertEquals(1, ap.getContractPartyList().getContractParties().size()),
                () -> assertEquals(ContractPartyRoleType.CONTRACTPARTNER, cp.getContractPartyRole()),
                () -> assertEquals("M", cp.getSalutation()),
                () -> assertEquals("VIGNAL", cp.getSurName()),
                () -> assertEquals("ANDRE", cp.getFirstName()),
                () -> assertNull(cp.getCompanyName()),
                () -> assertEquals("75112", cp.getIdentification()),
                () -> assertNull(cp.getEmail()),
                () -> assertNull(cp.getVATnumber()),
//endregion
//region Address
                () -> assertEquals(1, ap.getAddressList().getAddresses().size()),
                () -> assertEquals(AddressRoleType.DELIVERY, add.getAddressRole()),
                () -> assertEquals("75000", add.getPostalCode()),
                () -> assertNull(add.getCityName()),
                () -> assertEquals("12 rue de la Paix", add.getStreetName()),
                () -> assertEquals("Les Prés", add.getAddressSuffix())
//endregion
        );
    }

    @Test
    @SuppressWarnings("java:S5961")
    void accountingPointEnvelope_withMultipleSituations_emitsOnePointPerSituation() {
        // Given
        var consumption = new ContractualSituation(
                "3127069600", null, null, null,
                null, null, "Tarif BT<=36kVA", null, null, null, null, "6", List.of("C5"),
                null, null, null, null);
        var production = new ContractualSituation(
                "3127069601", null, null, null,
                null, null, "Tarif BT<=36kVA", null, null, null, null, "6", List.of("P4"),
                null, null, null, null);
        var generalData = new UsagePointGeneralData(
                new AddressData(new InstallationAddress(null,
                                                        null,
                                                        "1 rue de l'homologation",
                                                        null,
                                                        "60000",
                                                        "60600")));
        var intermediateAccountingPointDataMarketDocument = new IntermediateAccountingPointDataMarketDocument(
                new IdentifiableAccountingPointData(
                        permissionRequest(),
                        List.of(consumption, production),
                        generalData
                ),
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                        "fallbackId"
                )
        );

        // When
        var res = intermediateAccountingPointDataMarketDocument.accountingPointEnvelope();

        // Then
        var points = res.getAccountingPointMarketDocument().getAccountingPointList().getAccountingPoints();
        assertAll(
                () -> assertEquals(2, points.size()),
                () -> assertEquals("3127069600", points.getFirst().getMRID().getValue()),
                () -> assertEquals(DirectionTypeList.DOWN, points.getFirst().getDirection()),
                () -> assertEquals("3127069601", points.get(1).getMRID().getValue()),
                () -> assertEquals(DirectionTypeList.UP, points.get(1).getDirection())
        );
    }

    @Test
    @SuppressWarnings("java:S5961")
    void accountingPointEnvelope_withOrganization() {
        // Given
        var situation = new ContractualSituation(
                "3127069600", null, null, null,
                null, null, "Tarif BT<=36kVA", null, null, null, null, null, List.of("P4"),
                null,
                new ContactData("contact@example.com", null, null),
                null,
                new Organization("SNCF Immo", null, null, "12345678900000", "123456789"));
        var generalData = new UsagePointGeneralData(
                new AddressData(new InstallationAddress(
                        null, null, "1 rue de l'homologation", "Les Prés",
                        "60000", "60600")));
        var intermediateAccountingPointDataMarketDocument = new IntermediateAccountingPointDataMarketDocument(
                new IdentifiableAccountingPointData(
                        permissionRequest(),
                        List.of(situation),
                        generalData
                ),
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                        "fallbackId"
                )
        );

        // When
        var res = intermediateAccountingPointDataMarketDocument.accountingPointEnvelope();

        // Then
        var md = res.getAccountingPointMarketDocument();
        var ap = md.getAccountingPointList().getAccountingPoints().getFirst();
        var cp = ap.getContractPartyList().getContractParties().getFirst();
        assertAll(
                () -> assertEquals(DirectionTypeList.UP, ap.getDirection()),
                () -> assertEquals("SNCF Immo", cp.getCompanyName()),
                () -> assertEquals("12345678900000", cp.getVATnumber()),
                () -> assertEquals("contact@example.com", cp.getEmail()),
                () -> assertEquals("123456789", md.getReceiverMarketParticipantMRID().getValue()),
                () -> assertNull(cp.getSalutation()),
                () -> assertNull(cp.getSurName()),
                () -> assertNull(cp.getFirstName())
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
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC)
        );
    }
}