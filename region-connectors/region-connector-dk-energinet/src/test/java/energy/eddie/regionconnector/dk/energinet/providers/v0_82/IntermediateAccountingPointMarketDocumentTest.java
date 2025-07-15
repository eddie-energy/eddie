package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.dk.energinet.DtoLoader;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequestBuilder;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateAccountingPointMarketDocumentTest {
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "client-id"
    );

    @Test
    @SuppressWarnings("java:S5961")
    void testAccountingPointMarketDocument_isCorrectlyMapped() throws IOException {
        // Given
        var dto = DtoLoader.validApiResponse();
        var identifiableAp = new IdentifiableAccountingPointDetails(
                new EnerginetPermissionRequestBuilder()
                        .setPermissionId("pid")
                        .setConnectionId("cid")
                        .setDataNeedId("dnid")
                        .setMeteringPoint("mid")
                        .setStatus(PermissionProcessStatus.ACCEPTED)
                        .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                        .build(),
                Objects.requireNonNull(dto.getResult()).getFirst().getResult()
        );
        var intermediateAccountingPointMarketDocument = new IntermediateAccountingPointMarketDocument(identifiableAp,
                                                                                                      cimConfig);

        // When
        var res = intermediateAccountingPointMarketDocument.accountingPointMarketDocument()
                                                           .getAccountingPointMarketDocument();

        // Then
        assertAll(
                () -> assertNotNull(res.getMRID()),
                () -> assertEquals(CommonInformationModelVersions.V0_82.version(), res.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA, res.getType()),
                () -> assertNotNull(res.getCreatedDateTime()),
                () -> assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                                   res.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.CONSUMER, res.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(CodingSchemeTypeList.GS1, res.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(EnerginetRegionConnectorMetadata.GLOBAL_LOCATION_NUMBER,
                                   res.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   res.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals(cimConfig.eligiblePartyFallbackId(),
                                   res.getReceiverMarketParticipantMRID().getValue()),
                () -> assertFalse(res.getAccountingPointList().getAccountingPoints().isEmpty()),
                () -> {
                    var ap = res.getAccountingPointList().getAccountingPoints().getFirst();
                    assertAll(
                            () -> assertEquals("D01", ap.getSettlementMethod()),
                            () -> assertEquals("PT1H", ap.getMeterReadingResolution()),
                            () -> assertEquals("PT1H", ap.getResolution()),
                            () -> assertEquals(CommodityKind.ELECTRICITYPRIMARYMETERED, ap.getCommodity()),
                            () -> assertEquals(DirectionTypeList.DOWN, ap.getDirection()),
                            () -> assertEquals("E22", ap.getSupplyStatus()),
                            () -> assertEquals(CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME,
                                               ap.getMRID().getCodingScheme()),
                            () -> assertEquals("5713131XXXXXXXXXXX", ap.getMRID().getValue()),
                            () -> assertEquals(2, ap.getContractPartyList().getContractParties().size()),
                            () -> {
                                var contractParty = ap.getContractPartyList().getContractParties().getFirst();
                                assertAll(
                                        () -> assertEquals(ContractPartyRoleType.CONTRACTPARTNER,
                                                           contractParty.getContractPartyRole()),
                                        () -> assertEquals("John Doe", contractParty.getSurName()),
                                        () -> assertEquals("john@doe.com", contractParty.getEmail())
                                );
                            },
                            () -> assertEquals(3, ap.getAddressList().getAddresses().size()),
                            () -> {
                                var address = ap.getAddressList().getAddresses().getFirst();
                                assertAll(
                                        () -> assertEquals(AddressRoleType.DELIVERY, address.getAddressRole()),
                                        () -> assertEquals("1450", address.getPostalCode()),
                                        () -> assertEquals("København K", address.getCityName()),
                                        () -> assertEquals("1234 Street", address.getStreetName()),
                                        () -> assertEquals("1A", address.getBuildingNumber()),
                                        () -> assertEquals("2", address.getFloorNumber()),
                                        () -> assertEquals("", address.getDoorNumber())
                                );
                            },
                            () -> {
                                var address = ap.getAddressList().getAddresses().get(1);
                                assertAll(
                                        () -> assertEquals(AddressRoleType.INVOICE, address.getAddressRole()),
                                        () -> assertEquals("1450", address.getPostalCode()),
                                        () -> assertEquals("København K", address.getCityName()),
                                        () -> assertEquals("Street", address.getStreetName()),
                                        () -> assertEquals("1A", address.getBuildingNumber()),
                                        () -> assertEquals("2", address.getFloorNumber()),
                                        () -> assertEquals("", address.getDoorNumber())
                                );
                            }
                    );
                }
        );
    }
}