package energy.eddie.regionconnector.cds.providers.ap;

import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.cds.openapi.model.AccountsEndpoint200ResponseAllOfAccountsInner;
import energy.eddie.regionconnector.cds.openapi.model.MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner;
import energy.eddie.regionconnector.cds.openapi.model.ServiceContractEndpoint200ResponseAllOfServiceContractsInner;
import energy.eddie.regionconnector.cds.openapi.model.ServicePointEndpoint200ResponseAllOfServicePointsInner;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class IntermediateAccountingPointDocumentTest {

    @Test
    // Cim mapping requires that amount of asserts sadly
    @SuppressWarnings("java:S5961")
    void testToAp_returnsAccountingPoint() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .build();
        var id = new IdentifiableAccountingPointData(
                pr,
                new IdentifiableAccountingPointData.Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()
                                        .customerNumber("customer-number")
                                        .accountType("business")
                                        .cdsAccountId("account-id")
                                        .accountNumber("account-number")
                                        .accountName("EDDIE Ltd")),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()
                                        .cdsServicecontractId("contract-id")
                                        .contractAddress("123 Main St - PARKING LOT\nAnytown, TX 11111")
                                        .serviceType("electric")
                                        .cdsAccountId("account-number")
                                        .cdsAccountId("account-id")),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()
                                        .cdsServicepointId("service-point-id")
                                        .addCurrentServicecontractsItem("contract-id")
                                        .servicepointAddress("123 Main St - PARKING LOT\nAnytown, TX 11111")),
                        List.of(
                                new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()
                                        .addCurrentServicepointsItem("service-point-id")
                                        .cdsMeterdeviceId("meter-id")
                        )
                )
        );
        var document = new IntermediateAccountingPointDocument(id);

        // When
        var res = document.toAp();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(AccountingPointEnvelope::getAccountingPointMarketDocument)
                .satisfies(ap -> {
                    assertThat(ap.getMRID()).isNotEmpty();
                    assertThat(ap.getRevisionNumber()).isEqualTo("0.82");
                    assertThat(ap.getType()).isEqualTo(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA);
                    assertThat(ap.getCreatedDateTime()).isNotEmpty();
                    assertThat(ap.getSenderMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.METERING_POINT_ADMINISTRATOR);
                    assertThat(ap.getReceiverMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.CONSUMER);
                    assertThat(ap.getSenderMarketParticipantMRID())
                            .satisfies(sender -> {
                                assertThat(sender.getCodingScheme()).isEqualTo(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME);
                                assertThat(sender.getValue()).isEqualTo("CDSC");
                            });
                    assertThat(ap.getReceiverMarketParticipantMRID())
                            .satisfies(receiver -> {
                                assertThat(receiver.getCodingScheme()).isEqualTo(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME);
                                assertThat(receiver.getValue()).isEqualTo("customer-number");
                            });
                })
                .extracting(AccountingPointMarketDocumentComplexType::getAccountingPointList)
                .extracting(AccountingPointMarketDocumentComplexType.AccountingPointList::getAccountingPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(AccountingPointComplexType.class))
                .singleElement()
                .satisfies(ap -> {
                    assertThat(ap.getCommodity()).isEqualTo(CommodityKind.ELECTRICITYPRIMARYMETERED);
                    assertThat(ap.getMRID())
                            .satisfies(mrid -> {
                                assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME);
                                assertThat(mrid.getValue()).isEqualTo("meter-id");
                            });
                    assertThat(ap.getContractPartyList().getContractParties())
                            .singleElement()
                            .satisfies(contractParty -> {
                                assertThat(contractParty.getContractPartyRole()).isEqualTo(ContractPartyRoleType.CONTRACTPARTNER);
                                assertThat(contractParty.getCompanyName()).isEqualTo("EDDIE Ltd");
                            });
                })
                .extracting(AccountingPointComplexType::getAddressList)
                .extracting(AccountingPointComplexType.AddressList::getAddresses)
                .asInstanceOf(InstanceOfAssertFactories.list(AddressComplexType.class))
                .hasSize(2)
                .satisfies(addresses -> {
                    assertThat(addresses.getFirst())
                            .satisfies(address -> {
                                assertThat(address.getAddressRole()).isEqualTo(AddressRoleType.DELIVERY);
                                assertThat(address.getPostalCode()).isEqualTo("11111");
                                assertThat(address.getCityName()).isEqualTo("Anytown");
                                assertThat(address.getStreetName()).isEqualTo("Main St - PARKING LOT");
                                assertThat(address.getBuildingNumber()).isEqualTo("123");
                                assertThat(address.getStaircaseNumber()).isNull();
                                assertThat(address.getFloorNumber()).isNull();
                                assertThat(address.getDoorNumber()).isNull();
                                assertThat(address.getAddressSuffix()).isNull();
                            });
                });
    }

    @Test
    void testToAp_withInvalidAddress_hasAddressInSuffix() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .build();
        var id = new IdentifiableAccountingPointData(
                pr,
                new IdentifiableAccountingPointData.Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()
                                        .customerNumber("customer-number")
                                        .accountType("business")
                                        .cdsAccountId("account-id")
                                        .accountNumber("account-number")
                                        .accountName("EDDIE Ltd")),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()
                                        .cdsServicecontractId("contract-id")
                                        .contractAddress("INVALID ADDRESS")
                                        .serviceType("electric")
                                        .cdsAccountId("account-number")
                                        .cdsAccountId("account-id")),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()
                                        .cdsServicepointId("service-point-id")
                                        .addCurrentServicecontractsItem("contract-id")
                                        .servicepointAddress("INVALID ADDRESS")),
                        List.of(
                                new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()
                                        .addCurrentServicepointsItem("service-point-id")
                                        .cdsMeterdeviceId("meter-id")
                        )
                )
        );
        var document = new IntermediateAccountingPointDocument(id);

        // When
        var res = document.toAp();

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
                .hasSize(2)
                .last()
                .extracting(AddressComplexType::getAddressSuffix)
                .isEqualTo("INVALID ADDRESS");
    }

    @Test
    void testToAp_withSingleNameName_returnsName() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .build();
        var id = new IdentifiableAccountingPointData(
                pr,
                new IdentifiableAccountingPointData.Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()
                                        .customerNumber("customer-number")
                                        .accountType("other")
                                        .cdsAccountId("account-id")
                                        .accountNumber("account-number")
                                        .accountName("John")),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()
                                        .cdsServicecontractId("contract-id")
                                        .contractAddress("123 Main St - PARKING LOT\nAnytown, TX 11111")
                                        .serviceType("electric")
                                        .cdsAccountId("account-number")
                                        .cdsAccountId("account-id")),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()
                                        .cdsServicepointId("service-point-id")
                                        .addCurrentServicecontractsItem("contract-id")
                                        .servicepointAddress("123 Main St - PARKING LOT\nAnytown, TX 11111")),
                        List.of(
                                new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()
                                        .addCurrentServicepointsItem("service-point-id")
                                        .cdsMeterdeviceId("meter-id")
                        )
                )
        );
        var document = new IntermediateAccountingPointDocument(id);

        // When
        var res = document.toAp();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(AccountingPointEnvelope::getAccountingPointMarketDocument)
                .extracting(AccountingPointMarketDocumentComplexType::getAccountingPointList)
                .extracting(AccountingPointMarketDocumentComplexType.AccountingPointList::getAccountingPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(AccountingPointComplexType.class))
                .singleElement()
                .extracting(AccountingPointComplexType::getContractPartyList)
                .extracting(AccountingPointComplexType.ContractPartyList::getContractParties)
                .asInstanceOf(InstanceOfAssertFactories.list(ContractPartyComplexType.class))
                .singleElement()
                .satisfies(contractParty -> {
                    assertThat(contractParty.getContractPartyRole()).isEqualTo(ContractPartyRoleType.CONTRACTPARTNER);
                    assertThat(contractParty.getSurName()).isEqualTo("John");
                });
    }

    @Test
    void testToAp_withName_returnsName() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .build();
        var id = new IdentifiableAccountingPointData(
                pr,
                new IdentifiableAccountingPointData.Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()
                                        .customerNumber("customer-number")
                                        .accountType("other")
                                        .cdsAccountId("account-id")
                                        .accountNumber("account-number")
                                        .accountName("John Doe")),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()
                                        .cdsServicecontractId("contract-id")
                                        .contractAddress("123 Main St - PARKING LOT\nAnytown, TX 11111")
                                        .serviceType("electric")
                                        .cdsAccountId("account-number")
                                        .cdsAccountId("account-id")),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()
                                        .cdsServicepointId("service-point-id")
                                        .addCurrentServicecontractsItem("contract-id")
                                        .servicepointAddress("123 Main St - PARKING LOT\nAnytown, TX 11111")),
                        List.of(
                                new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()
                                        .addCurrentServicepointsItem("service-point-id")
                                        .cdsMeterdeviceId("meter-id")
                        )
                )
        );
        var document = new IntermediateAccountingPointDocument(id);

        // When
        var res = document.toAp();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(AccountingPointEnvelope::getAccountingPointMarketDocument)
                .extracting(AccountingPointMarketDocumentComplexType::getAccountingPointList)
                .extracting(AccountingPointMarketDocumentComplexType.AccountingPointList::getAccountingPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(AccountingPointComplexType.class))
                .singleElement()
                .extracting(AccountingPointComplexType::getContractPartyList)
                .extracting(AccountingPointComplexType.ContractPartyList::getContractParties)
                .asInstanceOf(InstanceOfAssertFactories.list(ContractPartyComplexType.class))
                .singleElement()
                .satisfies(contractParty -> {
                    assertThat(contractParty.getContractPartyRole()).isEqualTo(ContractPartyRoleType.CONTRACTPARTNER);
                    assertThat(contractParty.getSurName()).isEqualTo("Doe");
                    assertThat(contractParty.getFirstName()).isEqualTo("John");
                });
    }

    @ParameterizedTest
    @MethodSource
    void testToAp_forDifferentCommodities(String serviceType, CommodityKind commodity) {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .build();
        var id = new IdentifiableAccountingPointData(
                pr,
                new IdentifiableAccountingPointData.Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()
                                        .customerNumber("customer-number")
                                        .accountType("business")
                                        .cdsAccountId("account-id")
                                        .accountNumber("account-number")
                                        .accountName("EDDIE Ltd")),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()
                                        .cdsServicecontractId("contract-id")
                                        .contractAddress("123 Main St - PARKING LOT\nAnytown, TX 11111")
                                        .serviceType(serviceType)
                                        .cdsAccountId("account-number")
                                        .cdsAccountId("account-id")),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()
                                        .cdsServicepointId("service-point-id")
                                        .addCurrentServicecontractsItem("contract-id")
                                        .servicepointAddress("123 Main St - PARKING LOT\nAnytown, TX 11111")),
                        List.of(
                                new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()
                                        .addCurrentServicepointsItem("service-point-id")
                                        .cdsMeterdeviceId("meter-id")
                        )
                )
        );
        var document = new IntermediateAccountingPointDocument(id);

        // When
        var res = document.toAp();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(AccountingPointEnvelope::getAccountingPointMarketDocument)
                .extracting(AccountingPointMarketDocumentComplexType::getAccountingPointList)
                .extracting(AccountingPointMarketDocumentComplexType.AccountingPointList::getAccountingPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(AccountingPointComplexType.class))
                .singleElement()
                .extracting(AccountingPointComplexType::getCommodity)
                .isEqualTo(commodity);
    }

    private static Stream<Arguments> testToAp_forDifferentCommodities() {
        return Stream.of(
                Arguments.of("electric", CommodityKind.ELECTRICITYPRIMARYMETERED),
                Arguments.of("natural_gas", CommodityKind.NATURALGAS),
                Arguments.of("water", CommodityKind.POTABLEWATER),
                Arguments.of("unknown", null)
        );
    }
}