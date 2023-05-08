package eddie.energy.regionconnector.at.ponton;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.EnergyDirection;
import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.TransmissionException;
import eddie.energy.regionconnector.api.v0.models.ConsumptionRecord;
import eddie.energy.regionconnector.at.eda.EdaAdapter;
import eddie.energy.regionconnector.at.models.CCMORequest;
import eddie.energy.regionconnector.at.models.CMRequestStatus;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
                .hostname("eddie.projekte.fh-hagenberg.at")
                .port(2600)
                .workFolder(workFolder.toString())
                .adapterId("Fabian-Eddie")
                .adapterVersion("1.0.0")
                .build();

        var outputStream = new PrintStream(new FileOutputStream(path + File.separator + "consumptionRecords.txt"));
        var adapter = new PontonXPAdapter(config);
        adapter.subscribeToConsumptionRecordPublisher(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(ConsumptionRecord consumptionRecord) {
                System.out.println("Received consumptionRecord from: " + consumptionRecord.getMeteringPoint() + " for: " + consumptionRecord.getStartDateTime());
                outputStream.println(consumptionRecord);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error: " + throwable.getMessage());
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
                System.out.println("Received CMRequestStatus: " + cmRequestStatus);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error: " + throwable.getMessage());
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
            System.out.println("Enter ReqDatType (1 for HistoricalMeteringData, 2 for MeteringData, 3 for MasterData):");
            var reqDatType = switch (reader.readLine()) {
                case "1" -> "HistoricalMeteringData";
                case "3" -> "MasterData";
                default -> "MeteringData";
            };

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
        }
        else {
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
}
