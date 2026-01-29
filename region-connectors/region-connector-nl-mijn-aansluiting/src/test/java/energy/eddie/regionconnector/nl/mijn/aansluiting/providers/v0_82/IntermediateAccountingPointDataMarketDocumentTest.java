package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoint;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoints;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.type.TypeReference;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class IntermediateAccountingPointDataMarketDocumentTest {
    private final JsonResourceObjectMapper<MeteringPoints> apMapper = new JsonResourceObjectMapper<>(new TypeReference<>() {});

    @ParameterizedTest
    @MethodSource("toAp_returnsAccountingPoints")
    // Mapping code requires that many assertions
    @SuppressWarnings("java:S5961")
    void toAp_returnsAccountingPoints(MeteringPoint.ProductEnum product, CommodityKind commodity) {
        // Given
        var meteringPoints = apMapper.loadTestJson("codeboek_response.json");
        meteringPoints.getMeteringPoints().getFirst().setProduct(product);
        var config = createConfig();
        var pr = createPermissionRequest();
        var data = new IdentifiableAccountingPointData(pr, meteringPoints.getMeteringPoints());
        var intermediateDoc = new IntermediateAccountingPointDataMarketDocument(data, config);

        // When
        var res = intermediateDoc.toAp();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(AccountingPointEnvelope::getAccountingPointMarketDocument)
                .satisfies(ap -> {
                    assertThat(ap.getMRID()).isNotNull();
                    assertThat(ap.getRevisionNumber()).isEqualTo(CommonInformationModelVersions.V0_82.version());
                    assertThat(ap.getType()).isEqualTo(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA);
                    assertThat(ap.getCreatedDateTime()).isNotNull();
                    assertThat(ap.getSenderMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.METERING_POINT_ADMINISTRATOR);
                    assertThat(ap.getReceiverMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.PARTY_CONNECTED_TO_GRID);
                    assertThat(ap.getSenderMarketParticipantMRID()).satisfies(mrid -> {
                        assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME);
                        assertThat(mrid.getValue()).isEqualTo("client-id");
                    });
                    assertThat(ap.getReceiverMarketParticipantMRID()).satisfies(mrid -> {
                        assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME);
                        assertThat(mrid.getValue()).isEqualTo("8716948000000");
                    });
                })
                .extracting(AccountingPointMarketDocumentComplexType::getAccountingPointList)
                .extracting(AccountingPointMarketDocumentComplexType.AccountingPointList::getAccountingPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(AccountingPointComplexType.class))
                .singleElement()
                .satisfies(acc -> {
                    assertThat(acc.getCommodity()).isEqualTo(commodity);
                    assertThat(acc.getMRID()).satisfies(mrid -> {
                        assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME);
                        assertThat(mrid.getValue()).isEqualTo("871694840000000000");
                    });
                })
                .extracting(AccountingPointComplexType::getAddressList)
                .extracting(AccountingPointComplexType.AddressList::getAddresses)
                .asInstanceOf(InstanceOfAssertFactories.list(AddressComplexType.class))
                .singleElement()
                .satisfies(address -> {
                    assertThat(address.getAddressRole()).isEqualTo(AddressRoleType.DELIVERY);
                    assertThat(address.getStreetName()).isEqualTo("example street");
                    assertThat(address.getCityName()).isEqualTo("Amsterdam");
                    assertThat(address.getPostalCode()).isEqualTo("9999AB");
                    assertThat(address.getDoorNumber()).isEqualTo("11");
                    assertThat(address.getAddressSuffix()).isNull();
                });
    }

    @Test
    void toAp_withoutAddress_returnsAccountingPoints() {
        // Given
        var meteringPoints = apMapper.loadTestJson("codeboek_response_without_address.json");
        var config = createConfig();
        var pr = createPermissionRequest();
        var data = new IdentifiableAccountingPointData(pr, meteringPoints.getMeteringPoints());
        var intermediateDoc = new IntermediateAccountingPointDataMarketDocument(data, config);

        // When
        var res = intermediateDoc.toAp();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(AccountingPointEnvelope::getAccountingPointMarketDocument)
                .extracting(AccountingPointMarketDocumentComplexType::getAccountingPointList)
                .extracting(AccountingPointMarketDocumentComplexType.AccountingPointList::getAccountingPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(AccountingPointComplexType.class))
                .singleElement()
                .extracting(AccountingPointComplexType::getAddressList)
                .extracting(AccountingPointComplexType.AddressList::getAddresses)
                .asInstanceOf(InstanceOfAssertFactories.list(AddressComplexType.class))
                .isEmpty();
    }

    @Test
    void toAp_withoutStreetNumber_returnsEmptyDoorNumber() {
        // Given
        var meteringPoints = apMapper.loadTestJson("codeboek_response.json");
        Objects.requireNonNull(meteringPoints.getMeteringPoints().getFirst().getAddress()).setStreetNumber(null);
        var config = createConfig();
        var pr = createPermissionRequest();
        var data = new IdentifiableAccountingPointData(pr, meteringPoints.getMeteringPoints());
        var intermediateDoc = new IntermediateAccountingPointDataMarketDocument(data, config);

        // When
        var res = intermediateDoc.toAp();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(AccountingPointEnvelope::getAccountingPointMarketDocument)
                .extracting(AccountingPointMarketDocumentComplexType::getAccountingPointList)
                .extracting(AccountingPointMarketDocumentComplexType.AccountingPointList::getAccountingPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(AccountingPointComplexType.class))
                .singleElement()
                .extracting(AccountingPointComplexType::getAddressList)
                .extracting(AccountingPointComplexType.AddressList::getAddresses)
                .asInstanceOf(InstanceOfAssertFactories.list(AddressComplexType.class))
                .singleElement()
                .extracting(AddressComplexType::getDoorNumber)
                .isNull();
    }

    private static Stream<Arguments> toAp_returnsAccountingPoints() {
        return Stream.of(
                Arguments.of(MeteringPoint.ProductEnum.ELK, CommodityKind.ELECTRICITYPRIMARYMETERED),
                Arguments.of(MeteringPoint.ProductEnum.GAS, CommodityKind.NATURALGAS),
                Arguments.of(MeteringPoint.ProductEnum.H2, null),
                Arguments.of(null, null)
        );
    }

    private static MijnAansluitingPermissionRequest createPermissionRequest() {
        var today = LocalDate.now(ZoneOffset.UTC);
        return new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "state",
                "codeVerifier",
                ZonedDateTime.now(ZoneOffset.UTC),
                today,
                today,
                Granularity.P1D,
                "11",
                "9999AB"
        );
    }

    private static MijnAansluitingConfiguration createConfig() {
        return new MijnAansluitingConfiguration(
                "key-id",
                "https://localhost",
                new ClientID("client-id"),
                new Scope(),
                URI.create("http://localhost"),
                "api-token",
                URI.create("http://localhost")
        );
    }
}