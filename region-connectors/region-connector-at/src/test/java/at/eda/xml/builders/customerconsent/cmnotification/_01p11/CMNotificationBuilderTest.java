package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ObjectFactory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingAddressBuilder;
import at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingHeaderBuilder;
import at.eda.xml.builders.helper.Sector;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CMNotificationBuilderTest {
    @Test
    public void testCMNotificationBuilder() {
        // Example of a correct implementation
        CMNotificationBuilder cmNotificationBuilder = new CMNotificationBuilder();
        MarketParticipantDirectoryBuilder mpDirBuilder = new MarketParticipantDirectoryBuilder();
        RoutingHeaderBuilder routingHeaderBuilder = new RoutingHeaderBuilder();
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();
        ResponseDataTypeBuilder responseDataTypeBuilder = new ResponseDataTypeBuilder();
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        cmNotificationBuilder
                .withMarketParticipantDirectory(
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
                                .withSector(Sector.ELECTRICITY)
                                .withDocumentMode(DocumentMode.PROD)
                                .withDuplicate(false)
                                .withSchemaVersion("01.10")
                                .withMessageCode("ANTWORT_CCMO")
                                .build()
                )
                .withProcessDirectory(
                        processDirBuilder
                                .withMessageId("GC100007201912170930001230001234567")
                                .withConversationId("GC100007201912170930001230012345678")
                                .withCMRequestId("IWRN74PW")
                                .withResponseData(List.of(
                                        responseDataTypeBuilder
                                                .withConsentId("AT999999201912171011121230023456789")
                                                .withMeteringPointId("AT9999990699900000000000206868100")
                                                .withResponseCode(List.of(99))
                                                .build()
                                )).build()
                ).build();
    }

    @Test
    public void testNullPointer() {
        ObjectFactory objectFactory = new ObjectFactory();
        CMNotificationBuilder cmNotificationBuilder1 = new CMNotificationBuilder();
        CMNotificationBuilder cmNotificationBuilder2 = new CMNotificationBuilder();

        // Assign no required attributes
        assertThrows(NullPointerException.class, cmNotificationBuilder1::build);

        // Assign only one required attribute
        assertThrows(NullPointerException.class, () -> cmNotificationBuilder1
                .withMarketParticipantDirectory(objectFactory.createMarketParticipantDirectory())
                .build());
        assertThrows(NullPointerException.class, () -> cmNotificationBuilder2
                .withProcessDirectory(objectFactory.createProcessDirectory())
                .build());
    }
}
