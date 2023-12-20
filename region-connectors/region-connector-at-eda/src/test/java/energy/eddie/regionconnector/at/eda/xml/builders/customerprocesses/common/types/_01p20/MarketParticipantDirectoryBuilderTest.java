package energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.ObjectFactory;
import energy.eddie.regionconnector.at.eda.xml.builders.helper.Sector;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketParticipantDirectoryBuilderTest {
    @Test
    void testMarketParticipantDirectoryBuilder() {
        // Example of a correct implementation
        RoutingHeaderBuilder routingHeaderBuilder = new RoutingHeaderBuilder();
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();
        MarketParticipantDirectoryBuilder mpDirBuilder = new MarketParticipantDirectoryBuilder()
                .withRoutingHeader(
                        routingHeaderBuilder
                                .withSender(
                                        routingAddressBuilder.withMessageAddress("RC100007").withAddressType(AddressType.EC_NUMBER).build()
                                ).withReceiver(
                                        routingAddressBuilder.withMessageAddress("AT999999").withAddressType(AddressType.EC_NUMBER).build()
                                ).withDocCreationDateTime(
                                        LocalDateTime.of(2022, Month.DECEMBER, 17, 9, 30, 47)
                                ).build()
                )
                .withSector(Sector.ELECTRICITY)
                .withDocumentMode(DocumentMode.PROD)
                .withDuplicate(true);
        assertDoesNotThrow(mpDirBuilder::build);
    }

    @Test
    void testNullPointerException() {
        ObjectFactory objectFactory = new ObjectFactory();
        MarketParticipantDirectoryBuilder mpDirBuilder = new MarketParticipantDirectoryBuilder();

        // Assign no required arguments
        assertThrows(NullPointerException.class, mpDirBuilder::build);

        // Assign only one required argument
        assertThrows(NullPointerException.class, mpDirBuilder
                .withRoutingHeader(objectFactory.createRoutingHeader())
                ::build);

        // Assign only two required argument
        assertThrows(NullPointerException.class, mpDirBuilder
                .withSector(Sector.ELECTRICITY)
                ::build);

        // Assign only three required argument
        assertThrows(NullPointerException.class, mpDirBuilder
                .withDocumentMode(DocumentMode.SIMU)
                ::build);
    }
}
