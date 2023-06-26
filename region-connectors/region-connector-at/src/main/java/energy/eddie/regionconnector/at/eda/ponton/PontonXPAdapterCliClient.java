package energy.eddie.regionconnector.at.eda.ponton;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.ProcessDirectory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PropertiesAtConfiguration;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.requests.*;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.at.eda.utils.DateTimeConstants;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

public class PontonXPAdapterCliClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PontonXPAdapterCliClient.class);
    @Nullable
    private static AtConfiguration atConfiguration;

    public static void main(String[] args) throws ConnectionException, IOException, JAXBException, TransmissionException {
        var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        Properties properties = new Properties();
        var in = PontonXPAdapterCliClient.class.getClassLoader().getResourceAsStream("regionconnector-at.properties");
        properties.load(in);

        atConfiguration = PropertiesAtConfiguration.fromProperties(properties);
        PontonXPAdapterConfiguration config = PropertiesPontonXPAdapterConfiguration.fromProperties(properties);

        var outputStream = new PrintStream(new FileOutputStream(config.workFolder() + File.separator + "consumptionRecords.txt", true));
        var adapter = new PontonXPAdapter(config);
        var mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

        adapter.getConsumptionRecordStream().subscribe(consumptionRecord -> {
            LOGGER.info("Received consumptionRecord from: " + consumptionRecord.getProcessDirectory().getMeteringPoint() + " for: " + consumptionRecord.getProcessDirectory().getEnergy().get(0).getMeteringPeriodStart());
            try {
                outputStream.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(consumptionRecord));
            } catch (JsonProcessingException e) {
                LOGGER.error("Error while writing consumption record to file: ", e);
            }
        });

        adapter.getCMRequestStatusStream().subscribe(cmRequestStatus -> LOGGER.info("Received CMRequestStatus: " + cmRequestStatus));

        adapter.start();
        System.out.println("Adapter started");

        // request metering point from user in a loop
        var formatted = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        while (true) {
            try {

                System.out.println("Enter metering point (or 'exit' to quit):");
                var meteringPoint = reader.readLine();
                if (meteringPoint.equals("exit")) {
                    break;
                }

                // enter ReqDatType either HistoricalMeteringData or MeteringData
                System.out.println("Enter ReqDatType (1 for HistoricalMeteringData or MeteringData, 2 for MasterData, 3 for Revoke):");
                var datType = reader.readLine();
                if (datType.equals("3")) {
                    System.out.println("Enter consent id:");
                    var consentId = reader.readLine();

                    System.out.println("Enter end date (dd.MM.yyyy):");
                    var line = reader.readLine();
                    sendRevoke(adapter, consentId, meteringPoint, line.isBlank() ? null : LocalDate.parse(line, formatted));
                    continue;
                }

                var reqDatType = switch (datType) {
                    case "2" -> RequestDataType.MASTER_DATA;
                    default -> RequestDataType.METERING_DATA;
                };


                // read from date
                System.out.println("Enter from date (dd.MM.yyyy):");
                // read input and parse to OffsetDateTime
                var fromDate = LocalDate.parse(reader.readLine(), formatted);

                // read to date
                System.out.println("Enter to date (dd.MM.yyyy):");
                var toDate = LocalDate.parse(reader.readLine(), formatted);

                sendRequest(adapter, reader, meteringPoint, reqDatType, fromDate, toDate);
            } catch (Exception e) {
                System.err.println("Input error:" + e.getMessage());
            }
        }

        adapter.close();
        System.exit(0);
    }

    private static void sendRequest(EdaAdapter adapter, BufferedReader reader, String meteringPoint, RequestDataType reqDatType, LocalDate from, LocalDate to) throws JAXBException, IOException, InvalidDsoIdException {
        String receiverID = null;
        if (meteringPoint.isBlank()) {
            meteringPoint = null;
            // query for receiverID
            System.out.println("Enter DSO (e.g. AT003000):");
            receiverID = reader.readLine();
        }

        CCMOTimeFrame timeFrame = new CCMOTimeFrame(from, to);
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint(receiverID, meteringPoint);

        requireNonNull(atConfiguration, "atConfiguration must not be null");

        var ccmoRequest = new CCMORequest(dsoIdAndMeteringPoint, timeFrame, atConfiguration, reqDatType, AllowedMeteringIntervalType.QH, AllowedTransmissionCycle.D);
        var cmRequest = ccmoRequest.toCMRequest();

        System.out.println("RequestId: " + cmRequest.getProcessDirectory().getCMRequestId() + " ConversationId: " + cmRequest.getProcessDirectory().getConversationId());
        try {
            adapter.sendCMRequest(cmRequest);

        } catch (TransmissionException e) {
            LOGGER.error("Error sending CMRequest: " + e.getMessage(), e);
        }
    }

    private static void sendRevoke(EdaAdapter adapter, String consentId, String meteringPoint, @Nullable LocalDate end) {
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

        routingHeader.setDocumentCreationDateTime(datatypeFactory.newXMLGregorianCalendar(LocalDateTime.now(DateTimeConstants.AT_ZONE_ID).toString()));
        marketParticipant.setRoutingHeader(routingHeader);
        marketParticipant.setMessageCode(MessageCodes.Revoke.EligibleParty.REVOKE);
        marketParticipant.setSector("01");
        marketParticipant.setDocumentMode(DocumentMode.PROD);
        marketParticipant.setSchemaVersion(MessageCodes.Revoke.VERSION);
        cmRevoke.setMarketParticipantDirectory(marketParticipant);

        var pd = new ProcessDirectory();
        if (end != null) {
            pd.setConsentEnd(datatypeFactory.newXMLGregorianCalendar(end.toString()));
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
            LOGGER.error("Error sending CMRequest: " + e.getMessage(), e);
        }
    }
}

