package energy.eddie.regionconnector.at.eda;

import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.at.api.RegionConnectorAT;
import energy.eddie.regionconnector.at.api.SendCCMORequestResult;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PropertiesAtConfiguration;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PropertiesPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.requests.*;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import io.javalin.validation.JavalinValidation;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import static java.util.Objects.requireNonNull;

public class EdaRegionConnector implements RegionConnectorAT {

    public static final String COUNTRY_CODE = "at";
    public static final String MDA_CODE = COUNTRY_CODE + "-eda";
    public static final String MDA_DISPLAY_NAME = "Austria EDA";
    /**
     * The base path of the region connector. COUNTRY_CODE is enough, as in austria we only need one region connector
     */
    public static final String BASE_PATH = "/region-connectors/at-eda/";
    /**
     * The number of metering points covered by EDA, i.e. all metering points in Austria
     */
    public static final int COVERED_METERING_POINTS = 5977915;
    /**
     * DSOs in Austria are only allowed to store data for the last 36 months
     */
    public static final int MAXIMUM_MONTHS_IN_THE_PAST = 36;

    private final AtConfiguration atConfiguration;
    private final EdaAdapter edaAdapter;
    private final ConsumptionRecordMapper consumptionRecordMapper;
    private final EdaIdMapper edaIdMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRegionConnector.class);

    private final SubmissionPublisher<ConnectionStatusMessage> permissionStatusPublisher = new SubmissionPublisher<>();
    private final SubmissionPublisher<ConsumptionRecord> consumptionRecordSubmissionPublisher = new SubmissionPublisher<>();
    private final Javalin javalin = Javalin.create();

    /**
     * Workaround because ws are currently not working
     */
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();

    public EdaRegionConnector(AtConfiguration atConfiguration, EdaAdapter edaAdapter, EdaIdMapper edaIdMapper) throws TransmissionException {
        requireNonNull(atConfiguration);
        requireNonNull(edaAdapter);
        requireNonNull(edaIdMapper);

        this.atConfiguration = atConfiguration;
        this.edaAdapter = edaAdapter;
        this.consumptionRecordMapper = new ConsumptionRecordMapper(atConfiguration.timeZone());
        this.edaIdMapper = edaIdMapper;

        edaAdapter.getCMRequestStatusStream().subscribe(this::processCMRequestStatus);

        edaAdapter.getConsumptionRecordStream().subscribe(this::processConsumptionRecords);

        edaAdapter.start();
    }

    public EdaRegionConnector() throws IOException, JAXBException, ConnectionException, TransmissionException {
        Properties properties = new Properties();
        var in = EdaRegionConnector.class.getClassLoader().getResourceAsStream("regionconnector-at.properties");
        properties.load(in);

        this.atConfiguration = PropertiesAtConfiguration.fromProperties(properties);
        this.consumptionRecordMapper = new ConsumptionRecordMapper(atConfiguration.timeZone());

        PropertiesPontonXPAdapterConfiguration pontonXPAdapterConfig = PropertiesPontonXPAdapterConfiguration.fromProperties(properties);

        var workFolder = new File(pontonXPAdapterConfig.workFolder());
        if (workFolder.exists() || workFolder.mkdirs()) {
            System.out.println("Path exists: " + workFolder.getAbsolutePath());
        } else {
            throw new IOException("Could not create path: " + workFolder.getAbsolutePath());
        }

        this.edaAdapter = new PontonXPAdapter(pontonXPAdapterConfig);
        this.edaIdMapper = new InMemoryEdaIdMapper();


        this.edaAdapter.getCMRequestStatusStream().subscribe(this::processCMRequestStatus);
        this.edaAdapter.getConsumptionRecordStream().subscribe(this::processConsumptionRecords);
        this.edaAdapter.start();
    }

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        var in = EdaRegionConnector.class.getClassLoader().getResourceAsStream("regionconnector-at.properties");
        properties.load(in);

        try (RegionConnectorAT regionConnectorAT = new EdaRegionConnector()) {
            var hostname = properties.getProperty("hostname", "localhost");
            var port = Integer.parseInt(properties.getProperty("port", "8080"));

            regionConnectorAT.startWebapp(new InetSocketAddress(hostname, port), true);
            System.out.println("Started webapp on " + hostname + ":" + port);

            // wait for the user to press enter
            System.in.read();
        }
        System.exit(0);
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return consumptionRecordSubmissionPublisher;
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return permissionStatusPublisher;
    }

    @Override
    public void revokePermission(String permissionId) {
        throw new UnsupportedOperationException("Revoke permission is not yet implemented");
    }

    @Override
    public SendCCMORequestResult sendCCMORequest(String connectionId, CCMORequest request) throws TransmissionException, InvalidDsoIdException, JAXBException {
        requireNonNull(connectionId);
        requireNonNull(request);
        var cmRequest = request.toCMRequest();
        var permissionId = UUID.randomUUID().toString();
        edaIdMapper.addMappingInfo(cmRequest.getProcessDirectory().getConversationId(), cmRequest.getProcessDirectory().getCMRequestId(), new MappingInfo(permissionId, connectionId));

        edaAdapter.sendCMRequest(cmRequest);

        return new SendCCMORequestResult(permissionId, cmRequest.getProcessDirectory().getCMRequestId());
    }

    /**
     * Process an CMRequestStatus, convert it to a ConnectionStatusMessage and add connectionId and permissionId for identification before submitting it to the connection status message publisher
     *
     * @param cmRequestStatus the CMRequestStatus to process
     */
    private void processCMRequestStatus(CMRequestStatus cmRequestStatus) {
        var mappingInfo = edaIdMapper.getMappingInfoForConversationIdOrRequestID(cmRequestStatus.getConversationId(), cmRequestStatus.getCMRequestId().orElse(null));
        if (mappingInfo.isEmpty()) {
            // should not happen if a persistent mapping is used
            // TODO inform the administrative console if it happens
            LOGGER.warn("Received CMRequestStatus for unknown conversationId or requestId: {}", cmRequestStatus);
            return;
        }

        var permissionId = mappingInfo.get().permissionId();
        var connectionId = mappingInfo.get().connectionId();

        var message = cmRequestStatus.getMessage();
        var now = ZonedDateTime.now(atConfiguration.timeZone());

        var status = switch (cmRequestStatus.getStatus()) {
            case ACCEPTED -> ConnectionStatusMessage.Status.GRANTED;
            case ERROR -> ConnectionStatusMessage.Status.ERROR;
            case REJECTED -> ConnectionStatusMessage.Status.REJECTED;
            case SENT, RECEIVED, DELIVERED -> ConnectionStatusMessage.Status.REQUESTED;
        };
        var connectionStatusMessage = new ConnectionStatusMessage(connectionId, permissionId, now, status, message);
        permissionStatusPublisher.submit(connectionStatusMessage);
        // workaround because ws are currently not working
        permissionIdToConnectionStatusMessages.put(permissionId, connectionStatusMessage);

    }

    /**
     * Process an EDA consumption record, convert it to a CIM consumption record and add connectionId and permissionId for identification before submitting it to the consumption record publisher
     *
     * @param consumptionRecord the consumption record to process
     */
    private void processConsumptionRecords(at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.ConsumptionRecord consumptionRecord) {
        // this should only return null, if the consumption record was delivered in a separate EBUtilities process from the CCMO request i.e. when we request MeteringData (data from the future) and not HistoricalMeteringData
        // if we request MeteringData, we will receive a consumption record with a conversationId that we don't know
        // in this case the only way for us to identify which permission this consumption record belongs to is by checking if we have an ongoing permission for the metering point consumption
        // which also only works if we only have none overlapping permission periods per metering point
        // a metering point might also belong to multiple connection ids, so this is also not unique enough
        // i.e. future TODO rework mapper to support MeteringData
        var mappingInfo = edaIdMapper.getMappingInfoForConversationIdOrRequestID(consumptionRecord.getProcessDirectory().getConversationId(), null);
        var permissionId = mappingInfo.map(MappingInfo::permissionId).orElse(null);
        var connectionId = mappingInfo.map(MappingInfo::connectionId).orElse(null);

        try {
            ConsumptionRecord cimConsumptionRecord = consumptionRecordMapper.mapToCIM(consumptionRecord, permissionId, connectionId);
            consumptionRecordSubmissionPublisher.submit(cimConsumptionRecord);
        } catch (InvalidMappingException e) {
            // TODO In the future this should also inform the administrative console about the invalid mapping
            LOGGER.error("Could not map consumption record to CIM consumption record", e);
        }
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return new RegionConnectorMetadata(MDA_CODE, MDA_DISPLAY_NAME, COUNTRY_CODE, BASE_PATH, COVERED_METERING_POINTS);
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        JavalinValidation.register(ZonedDateTime.class, value -> value != null && !value.isBlank() ? LocalDate.parse(value, DateTimeFormatter.ISO_DATE).atStartOfDay(atConfiguration.timeZone()) : null);

        javalin.get(BASE_PATH + "/ce.js", context -> {
            context.contentType(ContentType.TEXT_JS);
            context.result(Objects.requireNonNull(getClass().getResourceAsStream("/public/ce.js")));
        });

        javalin.get(BASE_PATH + "/permission-status", ctx -> {
            var permissionId = ctx.queryParamAsClass("permissionId", String.class).get();
            var connectionStatusMessage = permissionIdToConnectionStatusMessages.get(permissionId);
            if (connectionStatusMessage == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }
            ctx.json(connectionStatusMessage);
        });

        javalin.post(BASE_PATH + "/permission-request", ctx -> {
            // TODO rework validation after mvp1
            var connectionIdValidator = ctx.formParamAsClass("connectionId", String.class)
                    .check(s -> s != null && !s.isBlank(), "connectionId must not be null or blank");

            var meteringPointIdValidator = ctx.formParamAsClass("meteringPointId", String.class)
                    .check(s -> s != null && s.length() == 33, "meteringPointId must be 33 characters long");

            var startValidator = ctx.formParamAsClass("start", ZonedDateTime.class)
                    .check(Objects::nonNull, "start must not be null")
                    .check(start -> start.isAfter(ZonedDateTime.now(start.getZone()).minusMonths(MAXIMUM_MONTHS_IN_THE_PAST)), "start must not be older than 36 months");

            var endValidator = ctx.formParamAsClass("end", ZonedDateTime.class)
                    //.allowNullable() // disable for now as we don't support Future data yet
                    .check(Objects::nonNull, "end must not be null")
                    .check(end -> end.isAfter(startValidator.get()), "end must be after start")
                    .check(end -> end.isBefore(ZonedDateTime.now(end.getZone()).minusDays(1)), "end must be in the past"); // for now, we only support historical data

            var errors = JavalinValidation.collectErrors(connectionIdValidator, meteringPointIdValidator, startValidator, endValidator);
            if (!errors.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(errors);
                return;
            }

            var start = startValidator.get();
            var end = Objects.requireNonNullElseGet(endValidator.get(), () -> ZonedDateTime.now(start.getZone()).minusDays(1));
            DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint(null, meteringPointIdValidator.get());

            var ccmoRequest = new CCMORequest(
                    dsoIdAndMeteringPoint,
                    new CCMOTimeFrame(start, end),
                    this.atConfiguration,
                    RequestDataType.METERING_DATA, // for now only allow metering data
                    AllowedMeteringIntervalType.QH,
                    AllowedTransmissionCycle.D);


            var connectionId = connectionIdValidator.get();

            var result = sendCCMORequest(connectionId, ccmoRequest);
            ctx.status(HttpStatus.OK);
            ctx.json(result);
        });

        javalin.exception(Exception.class, (e, ctx) -> {
            LOGGER.error("Exception occurred while processing request", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result("Internal Server Error");
        });

        javalin.start(address.getHostName(), address.getPort());

        return javalin.port();
    }


    @Override
    public void close() throws Exception {
        javalin.close();
        edaAdapter.close();
    }
}
