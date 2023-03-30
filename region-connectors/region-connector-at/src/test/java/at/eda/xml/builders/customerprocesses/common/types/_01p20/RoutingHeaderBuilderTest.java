package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.ObjectFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RoutingHeaderBuilderTest {
    @Test
    public void testRoutingHeaderBuilder() {
        // Example of a correct implementation
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();
        RoutingHeaderBuilder routingHeaderBuilder = new RoutingHeaderBuilder();

        routingHeaderBuilder
                .withSender(
                        routingAddressBuilder.withMessageAddress("AT999999").withAddressType(AddressType.EC_NUMBER).build()
                ).withReceiver(
                        routingAddressBuilder.withMessageAddress("GC100007").withAddressType(AddressType.OTHER).build()
                ).withDocCreationDateTime(
                        LocalDateTime.of(2020, Month.DECEMBER, 17, 9, 30, 47)
                ).build();
    }

    @Test
    public void testIllegalStateException() {
        ObjectFactory objectFactory = new ObjectFactory();
        RoutingHeaderBuilder routingHeaderBuilder = new RoutingHeaderBuilder();

        // Assign no required attributes
        assertThrows(IllegalStateException.class, routingHeaderBuilder::build);

        // Assign only one required attribute
        assertThrows(IllegalStateException.class, () -> routingHeaderBuilder
                .withSender(objectFactory.createRoutingAddress())
                .build());

        // Assign only one required attribute
        assertThrows(IllegalStateException.class, () -> routingHeaderBuilder
                .withReceiver(objectFactory.createRoutingAddress())
                .build());
    }
}
