package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.ObjectFactory;
import at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingAddressBuilder;
import at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingHeaderBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class MarketParticipantDirectoryBuilderTest {
    @Test
    public void testMarketParticipantDirectoryBuilder() {
        // Example of a correct implementation
        MarketParticipantDirectoryBuilder mpDirBuilder = new MarketParticipantDirectoryBuilder();
        RoutingHeaderBuilder routingHeaderBuilder = new RoutingHeaderBuilder();
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();

        mpDirBuilder
                .withRoutingHeader(
                        routingHeaderBuilder
                                .withSender(
                                        routingAddressBuilder.withMessageAddress("AT999999").withAddressType(AddressType.EC_NUMBER).build()
                                ).withReceiver(
                                        routingAddressBuilder.withMessageAddress("GC100007").withAddressType(AddressType.OTHER).build()
                                ).withDocCreationDateTime(
                                        LocalDateTime.of(2020, Month.DECEMBER, 17, 9, 30, 47)
                                ).build()
                )
                .withSector("01")
                .withDocumentMode(DocumentMode.PROD)
                .withDuplicate(false)
                .withSchemaVersion("01.10")
                .withMessageCode("ANTWORT_CCMO")
                .build();
    }

    @Test
    public void testEmptyString() {
        MarketParticipantDirectoryBuilder marketParticipantDirectoryBuilder = new MarketParticipantDirectoryBuilder();
        assertThrows(IllegalArgumentException.class, () -> marketParticipantDirectoryBuilder.withSector(""));
        assertThrows(IllegalArgumentException.class, () -> marketParticipantDirectoryBuilder.withMessageCode(""));
        assertThrows(IllegalArgumentException.class, () -> marketParticipantDirectoryBuilder.withSchemaVersion(""));
    }

    @Test
    public void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        MarketParticipantDirectoryBuilder marketParticipantDirectoryBuilder = new MarketParticipantDirectoryBuilder();

        // 20 Characters
        assertThrows(IllegalArgumentException.class, () -> marketParticipantDirectoryBuilder
                .withMessageCode("thisIsARandomStringWithOver20Characters"));
    }

    @Test
    public void testIllegalStateException() {
        ObjectFactory objectFactory = new ObjectFactory();
        MarketParticipantDirectoryBuilder mpDirBuilder = new MarketParticipantDirectoryBuilder();

        // Assign no required arguments
        assertThrows(IllegalStateException.class, mpDirBuilder::build);

        // Assign only one required argument
        assertThrows(IllegalStateException.class, () -> mpDirBuilder
                .withRoutingHeader(objectFactory.createRoutingHeader())
                .build());

        // Assign only two required argument
        assertThrows(IllegalStateException.class, () -> mpDirBuilder
                .withSector("01")
                .build());

        // Assign only three required argument
        assertThrows(IllegalStateException.class, () -> mpDirBuilder
                .withDocumentMode(DocumentMode.PROD)
                .build());

        // Assign only four required argument
        assertThrows(IllegalStateException.class, () -> mpDirBuilder
                .withDuplicate(false)
                .build());

        // Assign only five required argument
        assertThrows(IllegalStateException.class, () -> mpDirBuilder
                .withSchemaVersion("01.10")
                .build());
    }
}
