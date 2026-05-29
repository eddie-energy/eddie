// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.ap.AddressRoleType;
import energy.eddie.cim.v0_82.ap.CodingSchemeTypeList;
import energy.eddie.cim.v0_82.ap.CommodityKind;
import energy.eddie.cim.v0_82.ap.ContractPartyRoleType;
import energy.eddie.cim.v0_82.ap.DirectionTypeList;
import energy.eddie.cim.v0_82.ap.MessageTypeList;
import energy.eddie.cim.v0_82.ap.RoleTypeList;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusAccountingPointData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableAccountingPointData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IntermediateAccountingPointDataMarketDocumentTest {

    private static final String METERING_POINT_ID = "DE0001234567890123456789012345";
    private static final String ELIGIBLE_PARTY_ID = "test-ep-id";

    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME,
            "unused-fallback"
    );

    private final DeEtaPlusConfiguration deConfiguration = new DeEtaPlusConfiguration(
            ELIGIBLE_PARTY_ID,
            "http://api.url", "client-id", "client-secret",
            "/meters/historical", "/meters/accounting-point", "/v1/permissions/{id}", 30,
            3, 2, true, false,
            null, null
    );

    private final XmlMessageSerde serde = newSerde();

    private static XmlMessageSerde newSerde() {
        try {
            return new XmlMessageSerde();
        } catch (SerdeInitializationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static DePermissionRequest permissionRequest() {
        return new DePermissionRequestBuilder()
                .permissionId("perm-1")
                .connectionId("conn-1")
                .meteringPointId(METERING_POINT_ID)
                .start(LocalDate.of(2024, 1, 1))
                .end(LocalDate.of(2024, 12, 31))
                .dataNeedId("dn-ap")
                .build();
    }

    private AccountingPointEnvelope toEnvelope(EtaPlusAccountingPointData payload) {
        var envelope = new IntermediateAccountingPointDataMarketDocument(
                cimConfig, deConfiguration,
                new IdentifiableAccountingPointData(permissionRequest(), payload)
        ).toEnvelope();
        assertValidatesAgainstXsd(envelope);
        return envelope;
    }

    private void assertValidatesAgainstXsd(AccountingPointEnvelope envelope) {
        String xml;
        try {
            xml = new String(serde.serialize(envelope), StandardCharsets.UTF_8);
        } catch (SerializationException e) {
            throw new AssertionError("Failed to serialize AccountingPointEnvelope", e);
        }
        assertThat(XmlValidator.validateAccountingPointMarketDocument(xml))
                .as("Serialized envelope must validate against the AccountingPoint XSD:\n%s", xml)
                .isTrue();
    }

    @Test
    void toEnvelope_fullElectricityPayload_emitsCompletedDocument() {
        EtaPlusAccountingPointData payload = new EtaPlusAccountingPointData(
                METERING_POINT_ID, "42", "ELECTRICITY", "Consumption",
                new EtaPlusAccountingPointData.Address(
                        "Hauptstraße", "12", "10115", "Berlin", "DE", "Hinterhof"),
                new EtaPlusAccountingPointData.ContractParty(
                        "Max", "Mustermann", null, "max@example.de")
        );

        AccountingPointEnvelope envelope = toEnvelope(payload);
        var doc = envelope.getAccountingPointMarketDocument();

        assertThat(doc.getType()).isEqualTo(MessageTypeList.ACCOUNTING_POINT_MASTER_DATA);
        assertThat(doc.getSenderMarketParticipantMarketRoleType())
                .isEqualTo(RoleTypeList.METERING_POINT_ADMINISTRATOR);
        assertThat(doc.getReceiverMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.CONSUMER);
        assertThat(doc.getReceiverMarketParticipantMRID().getValue()).isEqualTo(ELIGIBLE_PARTY_ID);
        assertThat(doc.getSenderMarketParticipantMRID().getCodingScheme())
                .isEqualTo(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME);
        assertThat(doc.getCreatedDateTime()).isNotBlank();

        assertThat(doc.getAccountingPointList().getAccountingPoints()).hasSize(1);
        var point = doc.getAccountingPointList().getAccountingPoints().get(0);
        assertThat(point.getMRID().getValue()).isEqualTo(METERING_POINT_ID);
        assertThat(point.getMRID().getCodingScheme()).isEqualTo(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME);
        assertThat(point.getCommodity()).isEqualTo(CommodityKind.ELECTRICITYPRIMARYMETERED);
        assertThat(point.getDirection()).isEqualTo(DirectionTypeList.DOWN);

        var party = point.getContractPartyList().getContractParties().get(0);
        assertThat(party.getContractPartyRole()).isEqualTo(ContractPartyRoleType.CONTRACTPARTNER);
        assertThat(party.getFirstName()).isEqualTo("Max");
        assertThat(party.getSurName()).isEqualTo("Mustermann");
        assertThat(party.getCompanyName()).isNull();
        assertThat(party.getEmail()).isEqualTo("max@example.de");

        var addr = point.getAddressList().getAddresses().get(0);
        assertThat(addr.getAddressRole()).isEqualTo(AddressRoleType.DELIVERY);
        assertThat(addr.getStreetName()).isEqualTo("Hauptstraße 12");
        assertThat(addr.getPostalCode()).isEqualTo("10115");
        assertThat(addr.getCityName()).isEqualTo("Berlin");
        assertThat(addr.getAddressSuffix()).isEqualTo("Hinterhof");
    }

    @Test
    void toEnvelope_naturalGasMinimalPayload_omitsAbsentSubObjects() {
        EtaPlusAccountingPointData payload = new EtaPlusAccountingPointData(
                METERING_POINT_ID, null, "NATURAL_GAS", "Consumption", null, null
        );

        AccountingPointEnvelope envelope = toEnvelope(payload);
        var point = envelope.getAccountingPointMarketDocument()
                .getAccountingPointList().getAccountingPoints().get(0);

        assertThat(point.getCommodity()).isEqualTo(CommodityKind.NATURALGAS);
        assertThat(point.getDirection()).isEqualTo(DirectionTypeList.DOWN);
        assertThat(point.getContractPartyList().getContractParties()).isEmpty();
        assertThat(point.getAddressList().getAddresses()).isEmpty();
    }

    @Test
    void toEnvelope_prosumerCompany_emitsUpDirectionAndCompanyName() {
        EtaPlusAccountingPointData payload = new EtaPlusAccountingPointData(
                METERING_POINT_ID, "4711", "ELECTRICITY", "Generation",
                new EtaPlusAccountingPointData.Address(
                        "Marienplatz", "1", "80331", "München", "DE", null),
                new EtaPlusAccountingPointData.ContractParty(
                        null, null, "Solar Beispiel GmbH", "ap@solar-beispiel.de")
        );

        AccountingPointEnvelope envelope = toEnvelope(payload);
        var point = envelope.getAccountingPointMarketDocument()
                .getAccountingPointList().getAccountingPoints().get(0);

        assertThat(point.getDirection()).isEqualTo(DirectionTypeList.UP);
        var party = point.getContractPartyList().getContractParties().get(0);
        assertThat(party.getCompanyName()).isEqualTo("Solar Beispiel GmbH");
        assertThat(party.getFirstName()).isNull();
        assertThat(party.getSurName()).isNull();

        var addr = point.getAddressList().getAddresses().get(0);
        assertThat(addr.getStreetName()).isEqualTo("Marienplatz 1");
        assertThat(addr.getAddressSuffix()).isNull();
    }

    @Test
    void toEnvelope_unknownDirectionAndEnergyType_omitsBoth() {
        EtaPlusAccountingPointData payload = new EtaPlusAccountingPointData(
                METERING_POINT_ID, null, "PLUTONIUM", "Sideways", null, null
        );

        AccountingPointEnvelope envelope = toEnvelope(payload);
        var point = envelope.getAccountingPointMarketDocument()
                .getAccountingPointList().getAccountingPoints().get(0);

        assertThat(point.getCommodity()).isNull();
        assertThat(point.getDirection()).isNull();
    }

    @Test
    void toEnvelope_messageHeaderCarriesPermissionMetadata() {
        EtaPlusAccountingPointData payload = new EtaPlusAccountingPointData(
                METERING_POINT_ID, null, "ELECTRICITY", "Consumption", null, null
        );

        AccountingPointEnvelope envelope = toEnvelope(payload);

        var header = envelope.getMessageDocumentHeader();
        assertThat(header.getMessageDocumentHeaderMetaInformation().getPermissionid()).isEqualTo("perm-1");
        assertThat(header.getMessageDocumentHeaderMetaInformation().getConnectionid()).isEqualTo("conn-1");
        assertThat(header.getMessageDocumentHeaderMetaInformation().getDataNeedid()).isEqualTo("dn-ap");
    }

    @Test
    void toEnvelope_addressWithoutBuildingNumber_streetNameUsedAsIs() {
        EtaPlusAccountingPointData payload = new EtaPlusAccountingPointData(
                METERING_POINT_ID, null, "ELECTRICITY", "Consumption",
                new EtaPlusAccountingPointData.Address(
                        "Hauptstraße", null, "10115", "Berlin", "DE", null),
                null
        );

        AccountingPointEnvelope envelope = toEnvelope(payload);
        var addr = envelope.getAccountingPointMarketDocument()
                .getAccountingPointList().getAccountingPoints().get(0)
                .getAddressList().getAddresses().get(0);

        assertThat(addr.getStreetName()).isEqualTo("Hauptstraße");
    }
}
