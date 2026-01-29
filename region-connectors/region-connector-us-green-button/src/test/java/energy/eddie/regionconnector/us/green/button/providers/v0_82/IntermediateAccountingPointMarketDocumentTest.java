// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.ap.*;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

class IntermediateAccountingPointMarketDocumentTest {


    @Test
    @SuppressWarnings("java:S5961")
    void toAps_returnsAccountingPointDataMarketDocument() throws FeedException {
        // Given
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var xml = XmlLoader.xmlStreamFromResource("/xml/retailcustomer/accounting_point_data.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var permissionRequest = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                                         .setConnectionId("cid")
                                                                         .setDataNeedId("dnid")
                                                                         .setCountryCode("US")
                                                                         .setCompanyId("company")
                                                                         .build();
        var intermediateAp = new IntermediateAccountingPointMarketDocument(
                new IdentifiableSyndFeed(permissionRequest, feed),
                marshaller
        );

        // When
        var res = intermediateAp.toAps();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(AccountingPointEnvelope::getAccountingPointMarketDocument)
                .satisfies(md -> {
                    assertThat(md.getMRID()).isNotNull();
                    assertThat(md.getRevisionNumber()).isEqualTo(CommonInformationModelVersions.V0_82.version());
                    assertThat(md.getType()).isEqualTo(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA);
                    assertThat(md.getCreatedDateTime()).isNotNull();
                    assertThat(md.getSenderMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.CONSUMER);
                    assertThat(md.getReceiverMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.METERING_POINT_ADMINISTRATOR);
                    assertThat(md.getSenderMarketParticipantMRID()).satisfies(mrid -> {
                        assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME);
                        assertThat(mrid.getValue()).isEqualTo("HEATHER HOMEOWNER");
                    });
                    assertThat(md.getReceiverMarketParticipantMRID()).satisfies(mrid -> {
                        assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME);
                        assertThat(mrid.getValue()).isEqualTo("GreenButton");
                    });
                })
                .extracting(AccountingPointMarketDocumentComplexType::getAccountingPointList)
                .extracting(AccountingPointMarketDocumentComplexType.AccountingPointList::getAccountingPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(AccountingPointComplexType.class))
                .hasSize(3)
                .first()
                .satisfies(ap -> {
                    assertThat(ap.getMRID()).satisfies(mrid -> {
                        assertThat(mrid.getCodingScheme()).isEqualTo(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME);
                        assertThat(mrid.getValue()).isEqualTo("1902731");
                    });
                    assertThat(ap.getContractPartyList().getContractParties())
                            .singleElement()
                            .satisfies(contractParty -> {
                                assertThat(contractParty.getContractPartyRole()).isEqualTo(ContractPartyRoleType.CONTRACTPARTNER);
                                assertThat(contractParty.getSurName()).isEqualTo("HEATHER HOMEOWNER");
                                assertThat(contractParty.getIdentification()).isEqualTo("1234");
                            });
                    assertThat(ap.getBillingData().getReferenceNumber()).isEqualTo("11300-00000");
                })
                .extracting(AccountingPointComplexType::getAddressList)
                .extracting(AccountingPointComplexType.AddressList::getAddresses)
                .asInstanceOf(list(AddressComplexType.class))
                .hasSize(3)
                .first()
                .satisfies(address -> {
                    assertThat(address.getCityName()).isEqualTo("ANYTOWN");
                    assertThat(address.getPostalCode()).isEqualTo("12345");
                    assertThat(address.getAddressSuffix()).isEqualTo("111 ANYWHERE ST");
                });
    }
}