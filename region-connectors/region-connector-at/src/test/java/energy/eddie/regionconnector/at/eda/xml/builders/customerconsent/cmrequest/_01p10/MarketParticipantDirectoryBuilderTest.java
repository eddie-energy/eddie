package energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.ObjectFactory;
import energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingAddressBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingHeaderBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.helper.Sector;
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
                                        routingAddressBuilder.withMessageAddress("RC100007").withAddressType(AddressType.EC_NUMBER).build()
                                ).withReceiver(
                                        routingAddressBuilder.withMessageAddress("AT999999").withAddressType(AddressType.EC_NUMBER).build()
                                ).withDocCreationDateTime(
                                        LocalDateTime.of(2022, Month.DECEMBER, 17, 9, 30, 47)
                                ).build()
                )
                .withSector(Sector.ELECTRICITY)
                .withDocumentMode(DocumentMode.PROD)
                .withDuplicate(true)
                .withSchemaVersion("01.10")
                .withMessageCode("ANFORDERUNG_CMQF")
                .build();
    }

    @Test
    public void testEmptyString() {
        MarketParticipantDirectoryBuilder marketParticipantDirectoryBuilder = new MarketParticipantDirectoryBuilder();
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
    public void testNullPointerException() {
        ObjectFactory objectFactory = new ObjectFactory();
        MarketParticipantDirectoryBuilder mpDirBuilder = new MarketParticipantDirectoryBuilder();

        // Assign no required arguments
        assertThrows(NullPointerException.class, mpDirBuilder::build);

        // Assign only one required argument
        assertThrows(NullPointerException.class, () -> mpDirBuilder
                .withRoutingHeader(objectFactory.createRoutingHeader())
                .build());

        // Assign only two required argument
        assertThrows(NullPointerException.class, () -> mpDirBuilder
                .withSector(Sector.ELECTRICITY)
                .build());

        // Assign only three required argument
        assertThrows(NullPointerException.class, () -> mpDirBuilder
                .withDocumentMode(DocumentMode.SIMU)
                .build());

        // Assign only four required argument
        assertThrows(NullPointerException.class, () -> mpDirBuilder
                .withDuplicate(false)
                .build());

        // Assign only five required argument
        assertThrows(NullPointerException.class, () -> mpDirBuilder
                .withSchemaVersion("01.10")
                .build());
    }
}
