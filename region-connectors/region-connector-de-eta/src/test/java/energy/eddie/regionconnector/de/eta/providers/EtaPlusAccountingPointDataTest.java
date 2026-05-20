package energy.eddie.regionconnector.de.eta.providers;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class EtaPlusAccountingPointDataTest {

    private final JsonMapper mapper = new JsonMapper();

    @Test
    void deserialize_fullElectricityDocument_allFieldsPresent() {
        String body = """
                {
                  "meteringPointId": "DE0001234567890123456789012345",
                  "customerId": "42",
                  "energyType": "ELECTRICITY",
                  "direction": "Consumption",
                  "deliveryAddress": {
                    "streetName": "Hauptstraße",
                    "buildingNumber": "12",
                    "postalCode": "10115",
                    "city": "Berlin",
                    "country": "DE",
                    "addressSuffix": "Hinterhof"
                  },
                  "contractParty": {
                    "firstName": "Max",
                    "surName": "Mustermann",
                    "email": "max@example.de"
                  }
                }
                """;

        EtaPlusAccountingPointData data = mapper.readValue(body, EtaPlusAccountingPointData.class);

        assertThat(data.meteringPointId()).isEqualTo("DE0001234567890123456789012345");
        assertThat(data.customerId()).isEqualTo("42");
        assertThat(data.energyType()).isEqualTo("ELECTRICITY");
        assertThat(data.direction()).isEqualTo("Consumption");

        EtaPlusAccountingPointData.Address addr = data.deliveryAddress();
        assertThat(addr).isNotNull();
        assertThat(addr.streetName()).isEqualTo("Hauptstraße");
        assertThat(addr.buildingNumber()).isEqualTo("12");
        assertThat(addr.postalCode()).isEqualTo("10115");
        assertThat(addr.city()).isEqualTo("Berlin");
        assertThat(addr.country()).isEqualTo("DE");
        assertThat(addr.addressSuffix()).isEqualTo("Hinterhof");

        EtaPlusAccountingPointData.ContractParty cp = data.contractParty();
        assertThat(cp).isNotNull();
        assertThat(cp.firstName()).isEqualTo("Max");
        assertThat(cp.surName()).isEqualTo("Mustermann");
        assertThat(cp.companyName()).isNull();
        assertThat(cp.email()).isEqualTo("max@example.de");
    }

    @Test
    void deserialize_minimalNaturalGasDocument_subObjectsAbsent() {
        String body = """
                {
                  "meteringPointId": "DE9876543210987654321098765432",
                  "energyType": "NATURAL_GAS",
                  "direction": "Consumption"
                }
                """;

        EtaPlusAccountingPointData data = mapper.readValue(body, EtaPlusAccountingPointData.class);

        assertThat(data.meteringPointId()).isEqualTo("DE9876543210987654321098765432");
        assertThat(data.customerId()).isNull();
        assertThat(data.energyType()).isEqualTo("NATURAL_GAS");
        assertThat(data.direction()).isEqualTo("Consumption");
        assertThat(data.deliveryAddress()).isNull();
        assertThat(data.contractParty()).isNull();
    }

    @Test
    void deserialize_prosumerCompany_companyNameWithoutFirstNameSurname() {
        String body = """
                {
                  "meteringPointId": "DE5555555555555555555555555555",
                  "customerId": "4711",
                  "energyType": "ELECTRICITY",
                  "direction": "Generation",
                  "deliveryAddress": {
                    "streetName": "Marienplatz",
                    "buildingNumber": "1",
                    "postalCode": "80331",
                    "city": "München",
                    "country": "DE"
                  },
                  "contractParty": {
                    "companyName": "Solar Beispiel GmbH",
                    "email": "ap@solar-beispiel.de"
                  }
                }
                """;

        EtaPlusAccountingPointData data = mapper.readValue(body, EtaPlusAccountingPointData.class);

        assertThat(data.deliveryAddress()).isNotNull();
        assertThat(data.deliveryAddress().addressSuffix()).isNull();

        EtaPlusAccountingPointData.ContractParty cp = data.contractParty();
        assertThat(cp).isNotNull();
        assertThat(cp.companyName()).isEqualTo("Solar Beispiel GmbH");
        assertThat(cp.firstName()).isNull();
        assertThat(cp.surName()).isNull();
        assertThat(cp.email()).isEqualTo("ap@solar-beispiel.de");
    }

    @Test
    void deserialize_unknownTopLevelField_ignoredForwardCompat() {
        String body = """
                {
                  "meteringPointId": "DE0001234567890123456789012345",
                  "energyType": "ELECTRICITY",
                  "futureField": "ignored-by-jackson"
                }
                """;

        EtaPlusAccountingPointData data = mapper.readValue(body, EtaPlusAccountingPointData.class);

        assertThat(data.meteringPointId()).isEqualTo("DE0001234567890123456789012345");
        assertThat(data.energyType()).isEqualTo("ELECTRICITY");
    }

    @Test
    void deserialize_unknownNestedAddressField_ignoredForwardCompat() {
        String body = """
                {
                  "meteringPointId": "DE0001234567890123456789012345",
                  "deliveryAddress": {
                    "streetName": "Hauptstraße",
                    "staircase": "A",
                    "floor": "2"
                  }
                }
                """;

        EtaPlusAccountingPointData data = mapper.readValue(body, EtaPlusAccountingPointData.class);

        assertThat(data.deliveryAddress()).isNotNull();
        assertThat(data.deliveryAddress().streetName()).isEqualTo("Hauptstraße");
    }

    @Test
    void deserialize_unknownNestedContractPartyField_ignoredForwardCompat() {
        String body = """
                {
                  "meteringPointId": "DE0001234567890123456789012345",
                  "contractParty": {
                    "firstName": "Max",
                    "salutation": "Herr",
                    "vatNumber": "DE123"
                  }
                }
                """;

        EtaPlusAccountingPointData data = mapper.readValue(body, EtaPlusAccountingPointData.class);

        assertThat(data.contractParty()).isNotNull();
        assertThat(data.contractParty().firstName()).isEqualTo("Max");
    }

    @Test
    void deserialize_explicitJsonNullLeaf_treatedAsAbsent() {
        String body = """
                {
                  "meteringPointId": "DE0001234567890123456789012345",
                  "customerId": null,
                  "deliveryAddress": {
                    "streetName": "Hauptstraße",
                    "city": null
                  }
                }
                """;

        EtaPlusAccountingPointData data = mapper.readValue(body, EtaPlusAccountingPointData.class);

        assertThat(data.customerId()).isNull();
        assertThat(data.deliveryAddress()).isNotNull();
        assertThat(data.deliveryAddress().streetName()).isEqualTo("Hauptstraße");
        assertThat(data.deliveryAddress().city()).isNull();
    }

    @Test
    void deserialize_subObjectExplicitNull_treatedAsAbsentSubObject() {
        String body = """
                {
                  "meteringPointId": "DE0001234567890123456789012345",
                  "deliveryAddress": null,
                  "contractParty": null
                }
                """;

        EtaPlusAccountingPointData data = mapper.readValue(body, EtaPlusAccountingPointData.class);

        assertThat(data.deliveryAddress()).isNull();
        assertThat(data.contractParty()).isNull();
    }
}