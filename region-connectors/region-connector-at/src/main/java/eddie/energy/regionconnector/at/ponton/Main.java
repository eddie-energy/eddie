package eddie.energy.regionconnector.at.ponton;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.MarketParticipantDirectory;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.ProcessDirectory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.ponton.xp.adapter.api.ConnectionException;
import eddie.energy.regionconnector.api.v0.models.ConsumptionRecord;
import eddie.energy.regionconnector.at.eda.EdaAdapter;
import eddie.energy.regionconnector.at.models.CCMORequest;
import eddie.energy.regionconnector.at.models.CMRequestStatus;
import eddie.energy.regionconnector.at.models.MessageCodes;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.concurrent.Flow;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws ConnectionException, IOException, JAXBException, eddie.energy.regionconnector.at.eda.TransmissionException {
        String path = System.getProperty("user.home") + File.separator + "pontonxp" + File.separator + "work";
        var workFolder = new File(path);
        if (workFolder.exists() || workFolder.mkdirs()) {
            System.out.println("Path exists: " + workFolder.getAbsolutePath());
        } else {
            throw new IOException("Could not create path: " + workFolder.getAbsolutePath());
        }
        PontonXPAdapterConfig config = new PontonXPAdapterConfig.Builder()
                .withHostname("eddie.projekte.fh-hagenberg.at")
                .withPort(2600)
                .withWorkFolder(workFolder.toString())
                .withAdapterId("Fabian-Eddie")
                .withAdapterVersion("1.0.0")
                .build();
        var outputStream = new PrintStream(new FileOutputStream(path + File.separator + "consumptionRecords.txt", true));
        var adapter = new PontonXPAdapter(config);
        var mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        adapter.subscribeToConsumptionRecordPublisher(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(ConsumptionRecord consumptionRecord) {
                logger.info("Received consumptionRecord from: " + consumptionRecord.getMeteringPoint() + " for: " + consumptionRecord.getStartDateTime());
                try {
                    outputStream.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(consumptionRecord));
                } catch (JsonProcessingException e) {
                    logger.error("Error while writing consumption record to file: ", e);
                }
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Error in consumption record stream: ", throwable);
                subscription.request(1);
            }

            @Override
            public void onComplete() {
                System.out.println("Completed");
            }
        });

        adapter.subscribeToCMRequestStatusPublisher(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(CMRequestStatus cmRequestStatus) {
                logger.info("Received CMRequestStatus: " + cmRequestStatus);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Error in status update stream: ", throwable);
            }

            @Override
            public void onComplete() {
                System.out.println("Completed");
            }
        });

        adapter.start();
        System.out.println("Adapter started");

        // request metering point from user in a loop
        var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        var formatted = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        int i = 0;
        while (true) {
            System.out.println("Enter metering point (or 'exit' to quit):");
            var meteringPoint = reader.readLine();
            if (meteringPoint.equals("exit")) {
                break;
            }

            // enter ReqDatType either HistoricalMeteringData or MeteringData
            System.out.println("Enter ReqDatType (1 for HistoricalMeteringData, 2 for MeteringData, 3 for MasterData, 4 for Revoke):");
            var reqDatType = switch (reader.readLine()) {
                case "1" -> "HistoricalMeteringData";
                case "3" -> "MasterData";
                case "4" -> "Revoke";
                default -> "MeteringData";
            };

            if(reqDatType.equals("Revoke")) {
                System.out.println("Enter consent id:");
                var consentId = reader.readLine();

                System.out.println("Enter end date (dd.MM.yyyy):");
                var line = reader.readLine();
                sendRevoke(adapter, consentId,meteringPoint, line.isBlank() ? null:  OffsetDateTime.of(LocalDate.parse(line, formatted).atStartOfDay(), ZoneOffset.UTC));
                continue;
            }

            // read from date
            System.out.println("Enter from date (dd.MM.yyyy):");
            // read input and parse to OffsetDateTime
            var fromDate = LocalDate.parse(reader.readLine(), formatted);

            // read to date
            System.out.println("Enter to date (dd.MM.yyyy):");
            var toDate = LocalDate.parse(reader.readLine(), formatted);

            sendRequest(adapter, reader, String.valueOf(i), meteringPoint, reqDatType, OffsetDateTime.of(fromDate.atStartOfDay(), ZoneOffset.UTC), OffsetDateTime.of(toDate.atStartOfDay(), ZoneOffset.UTC));
            i++;
        }

        adapter.close();
    }

    private static void sendRequest(EdaAdapter adapter, BufferedReader reader, String connectionId, String meteringPoint, String reqDatType, OffsetDateTime from, OffsetDateTime to) throws JAXBException, IOException {
        var senderID = "EP100129";
        String receiverID;
        if (!meteringPoint.isBlank()) {
            receiverID = meteringPoint.substring(0, 8);
        } else {
            // query for receiverID
            System.out.println("Enter DSO (e.g. AT003000):");
            receiverID = reader.readLine();
        }


        var ccmoRequest = new CCMORequest(connectionId, senderID, receiverID, from, to);
        ccmoRequest.withMeteringPoint(meteringPoint)
                .withRequestDataType(reqDatType);
        var cmRequest = ccmoRequest.toCMRequest();

        System.out.println("RequestId: " + cmRequest.getProcessDirectory().getCMRequestId() + " ConversationId: " + cmRequest.getProcessDirectory().getConversationId());
        try {
            adapter.sendCMRequest(cmRequest);

        } catch (eddie.energy.regionconnector.at.eda.TransmissionException e) {
            logger.error("Error sending CMRequest: " + e.getMessage(), e);
        }
    }

    private static void sendRevoke(EdaAdapter adapter, String consentId, String meteringPoint, OffsetDateTime end) {
        CMRevoke cmRevoke = new CMRevoke();
        DatatypeFactory datatypeFactory = DatatypeFactory.newDefaultInstance();
        var senderID = "EP100129";
        String receiverID = meteringPoint.substring(0, 8);
        var sender = new RoutingAddress();
        sender.setAddressType(AddressType.EC_NUMBER);
        sender.setMessageAddress(senderID);
        var receiver = new RoutingAddress();
        receiver.setAddressType(AddressType.EC_NUMBER);
        receiver.setMessageAddress(receiverID);

        var marketParticipant = new at.ebutilities.schemata.customerconsent.cmrevoke._01p00.MarketParticipantDirectory();
        var routingHeader = new RoutingHeader();
        routingHeader.setSender(sender);
        routingHeader.setReceiver(receiver);

        routingHeader.setDocumentCreationDateTime(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now(ZoneOffset.UTC))));
        marketParticipant.setRoutingHeader(routingHeader);
        marketParticipant.setMessageCode(MessageCodes.Revoke.EligibleParty.REVOKE);
        marketParticipant.setSector("01");
        marketParticipant.setDocumentMode(DocumentMode.PROD);
        marketParticipant.setSchemaVersion(MessageCodes.Revoke.VERSION);
        cmRevoke.setMarketParticipantDirectory(marketParticipant);

        var pd = new ProcessDirectory();
        if (end != null) {
            pd.setConsentEnd(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(end.toZonedDateTime())));
        }
        pd.setConsentId(consentId);
        pd.setMeteringPoint(meteringPoint);
        var id = senderID + "T" + Instant.now().toEpochMilli();
        pd.setMessageId(id);
        pd.setConversationId(id);
        cmRevoke.setProcessDirectory(pd);

        try {
            adapter.sendCMRevoke(cmRevoke);
        } catch (Exception e) {
            logger.error("Error sending CMRequest: " + e.getMessage(), e);
        }
    }
}
