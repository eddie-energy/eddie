package energy.eddie.regionconnector.fi.fingrid.services.cim.v0_82;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.services.cim.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

class IntermediateAccountingPointDataMarketDocumentTest {
    @SuppressWarnings("java:S5961") // CIM mapping requires many assertions
    @Test
    void toAp_returnsAccountingPointDataMarketDocument() {
        // Given
        var customerData = TestResourceProvider.readCustomerDataFromFile(TestResourceProvider.CUSTOMER_DATA_JSON);
        var header = customerData.customerData().header();
        var transaction = customerData.customerData().transaction();
        var agreement = transaction.agreements().getFirst();
        var meter = agreement.meteringPoint();
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .build();
        var id = new IdentifiableAccountingPointData(pr, customerData);
        var intermediate = new IntermediateAccountingPointDataMarketDocument(id);

        // When
        var res = intermediate.toAp().getAccountingPointMarketDocument();

        // Then
        assertThat(res)
                .satisfies(doc -> {
                    assertThat(doc.getMRID()).isEqualTo(header.identification());
                    assertThat(doc.getRevisionNumber()).isEqualTo(CommonInformationModelVersions.V0_82.version());
                    assertThat(doc.getType()).isEqualTo(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA);
                    assertThat(doc.getCreatedDateTime()).isEqualTo(new EsmpDateTime(header.creation()).toString());
                    assertThat(doc.getSenderMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.METERING_POINT_ADMINISTRATOR);
                    assertThat(doc.getReceiverMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.CONSUMER);
                    assertThat(doc.getSenderMarketParticipantMRID()).satisfies(mrid -> {
                        assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME);
                        assertThat(mrid.getValue()).isEqualTo(header.juridicalSenderParty().identification());
                    });
                    assertThat(doc.getReceiverMarketParticipantMRID()).satisfies(mrid -> {
                        assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME);
                        assertThat(mrid.getValue()).isEqualTo(header.juridicalReceiverParty().identification());
                    });
                })
                .extracting(AccountingPointMarketDocumentComplexType::getAccountingPointList)
                .extracting(AccountingPointMarketDocumentComplexType.AccountingPointList::getAccountingPoints)
                .asInstanceOf(list(AccountingPointComplexType.class))
                .singleElement()
                .satisfies(ap -> {
                    assertThat(ap.getMRID()).satisfies(mrid -> {
                        assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME);
                        assertThat(mrid.getValue()).isEqualTo(agreement.meteringPoint().meteringPointEAN());
                    });
                    assertThat(ap.getContractPartyList().getContractParties())
                            .singleElement()
                            .satisfies(contractParty -> {
                                assertThat(contractParty.getContractPartyRole()).isEqualTo(ContractPartyRoleType.CONTRACTPARTNER);
                                assertThat(contractParty.getCompanyName()).isEqualTo(transaction.companyName());
                                assertThat(contractParty.getIdentification()).isEqualTo(transaction.customerIdentification());
                                assertThat(contractParty.getDateOfBirth()).isNull();
                                assertThat(contractParty.getEmail()).isEqualTo(transaction.emailAddress());
                            });
                })
                .extracting(AccountingPointComplexType::getAddressList)
                .extracting(AccountingPointComplexType.AddressList::getAddresses)
                .asInstanceOf(list(AddressComplexType.class))
                .satisfies(addresses -> {
                    var meterAddress = addresses.getFirst();
                    var customerAddress = addresses.getLast();
                    assertThat(meterAddress).satisfies(address -> {
                        assertThat(address.getAddressRole()).isEqualTo(AddressRoleType.DELIVERY);
                        assertThat(address.getPostalCode()).isEqualTo(meter.meteringPointAddress().postalCode());
                        assertThat(address.getCityName()).isEqualTo(meter.meteringPointAddress().postOffice());
                        assertThat(address.getStreetName()).isEqualTo(meter.meteringPointAddress().streetName());
                        assertThat(address.getBuildingNumber()).isEqualTo(meter.meteringPointAddress()
                                                                               .buildingNumber());
                        assertThat(address.getStaircaseNumber()).isEqualTo(meter.meteringPointAddress()
                                                                                .stairwellIdentification());
                        assertThat(address.getAddressSuffix()).isEqualTo(meter.meteringPointAddress().addressNote());
                        assertThat(address.getDoorNumber()).isEqualTo(meter.meteringPointAddress().apartment());
                    });
                    assertThat(customerAddress).satisfies(address -> {
                        assertThat(address.getAddressRole()).isEqualTo(AddressRoleType.INVOICE);
                        assertThat(address.getPostalCode()).isEqualTo(transaction.customerPostalAddress().postalCode());
                        assertThat(address.getCityName()).isEqualTo(transaction.customerPostalAddress().postOffice());
                        assertThat(address.getStreetName()).isEqualTo(transaction.customerPostalAddress().streetName());
                        assertThat(address.getBuildingNumber()).isEqualTo(transaction.customerPostalAddress()
                                                                                     .buildingNumber());
                        assertThat(address.getStaircaseNumber()).isEqualTo(transaction.customerPostalAddress()
                                                                                      .stairwellIdentification());
                        assertThat(address.getAddressSuffix()).isEqualTo(transaction.customerPostalAddress()
                                                                                    .addressNote());
                        assertThat(address.getDoorNumber()).isEqualTo(transaction.customerPostalAddress().apartment());
                    });
                });
    }
}