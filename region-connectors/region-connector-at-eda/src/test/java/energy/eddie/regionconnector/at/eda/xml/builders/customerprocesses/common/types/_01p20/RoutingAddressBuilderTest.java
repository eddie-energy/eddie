package energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RoutingAddressBuilderTest {
    @Test
    public void testRoutingAddressBuilder() {
        // Example of a correct implementation
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();

        routingAddressBuilder
                .withMessageAddress("AT999999")
                .withAddressType(AddressType.EC_NUMBER)
                .build();
    }

    @Test
    public void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();

        // 8 Characters
        assertThrows(IllegalArgumentException.class, () -> routingAddressBuilder
                .withMessageAddress("AT9999998"));
    }

    @Test
    public void testStringWrongCharacters() {
        // Assign string which contains not allowed characters and wrong character order
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();

        assertThrows(IllegalArgumentException.class, () -> routingAddressBuilder
                .withMessageAddress("!§$&/(()="));
        assertThrows(IllegalArgumentException.class, () -> routingAddressBuilder
                .withMessageAddress("SpecialCharacters!\"-.,"));
        // Wrong order of characters
        assertThrows(IllegalArgumentException.class, () -> routingAddressBuilder
                .withMessageAddress("A999999T"));
        assertThrows(IllegalArgumentException.class, () -> routingAddressBuilder
                .withMessageAddress("999999AT"));
    }

    @Test
    public void testNullPointerException() {
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();

        // Assign no required attributes
        assertThrows(NullPointerException.class, routingAddressBuilder::build);

        // Assign only one required attribute
        assertThrows(NullPointerException.class, () -> routingAddressBuilder.withMessageAddress("AT999999").build());
    }

    @Test
    public void testEmptyString() {
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();

        assertThrows(IllegalArgumentException.class, () -> routingAddressBuilder.withMessageAddress(""));
    }
}
