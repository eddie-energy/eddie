package energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.EnergyDirection;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ObjectFactory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingAddressBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.RoutingHeaderBuilder;
import energy.eddie.regionconnector.at.eda.xml.builders.helper.Sector;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CMRequestBuilderTest {
    @Test
    public void testCMRequestBuilder() {
        // Example of a correct implementation
        CMRequestBuilder cmRequestBuilder = new CMRequestBuilder();
        MarketParticipantDirectoryBuilder mpDirBuilder = new MarketParticipantDirectoryBuilder();
        RoutingHeaderBuilder routingHeaderBuilder = new RoutingHeaderBuilder();
        RoutingAddressBuilder routingAddressBuilder = new RoutingAddressBuilder();
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();
        ReqTypeBuilder reqTypeBuilder = new ReqTypeBuilder();

        cmRequestBuilder
                .withMarketParticipantDirectory(
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
                                .build()
                )
                .withProcessDirectory(
                        processDirBuilder
                                .withMessageId("GC100007201912170930001230001234567")
                                .withConversationId("GC100007201912170930001230012345678")
                                .withProcessDate(LocalDate.of(2019, Month.DECEMBER, 17))
                                .withMeteringPoint("AT9999990699900000000000206868100")
                                .withCMRequestId("IWRN74PW")
                                .withConsentId("AT999999201912171011121230023456789")
                                .withCMRequest(
                                        reqTypeBuilder
                                                .withReqDatType("EnergyCommunityRegistration")
                                                .withDateFrom(LocalDate.of(2022, Month.DECEMBER, 18))
                                                .withEcId("AT99999900000RC000000000012345678")
                                                .withEnergyDirection(EnergyDirection.CONSUMPTION)
                                                .build()
                                ).build()
                ).build();
    }

    @Test
    public void testNullPointerException() {
        ObjectFactory objectFactory = new ObjectFactory();
        CMRequestBuilder cmRequestBuilder1 = new CMRequestBuilder();
        CMRequestBuilder cmRequestBuilder2 = new CMRequestBuilder();

        // Assign no required attributes
        assertThrows(NullPointerException.class, cmRequestBuilder1::build);

        // Assign only one required attribute
        assertThrows(NullPointerException.class, () -> cmRequestBuilder1
                .withMarketParticipantDirectory(objectFactory.createMarketParticipantDirectory())
                .build());
        assertThrows(NullPointerException.class, () -> cmRequestBuilder2
                .withProcessDirectory(objectFactory.createProcessDirectory())
                .build());
    }
}
