package energy.eddie.regionconnector.dk.energinet.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import energy.eddie.regionconnector.dk.energinet.services.PermissionRequestService;
import feign.FeignException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Sinks;

import java.beans.PropertyEditorSupport;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.BASE_PATH;


@RestController
@RequestMapping(BASE_PATH)
public class PermissionRequestController {
    private static final String CE_JS = "ce.js";
    /*
    We have to check two different paths depending on if the Region-Connector is run by the core or in standalone.
     */
    private static final String[] CE_DEV_PATHS = new String[]{
            "./region-connectors/region-connector-dk-energinet/src/main/resources/public" + BASE_PATH + CE_JS,
            "./src/main/resources/public" + BASE_PATH + CE_JS
    };
    private static final String CE_PRODUCTION_PATH = "/public" + BASE_PATH + CE_JS;
    private static final String PERMISSION_STATUS_PATH = "/permission-status";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final Environment environment;
    private final PermissionRequestService service;
    private final PermissionRequestFactory requestFactory;
    private final EnerginetCustomerApi energinetCustomerApi;
    private final TaskExecutor fetchRecordsExecutor;
    private final Sinks.Many<ConsumptionRecord> consumptionRecordSink;

    @Autowired
    public PermissionRequestController(
            Environment environment,
            PermissionRequestService service,
            PermissionRequestFactory requestFactory,
            EnerginetCustomerApi energinetCustomerApi,
            TaskExecutor fetchRecordsExecutor,
            Sinks.Many<ConsumptionRecord> consumptionRecordSink) {
        this.environment = environment;
        this.service = service;
        this.requestFactory = requestFactory;
        this.energinetCustomerApi = energinetCustomerApi;
        this.fetchRecordsExecutor = fetchRecordsExecutor;
        this.consumptionRecordSink = consumptionRecordSink;
    }

    private static String findCEDevPath() throws FileNotFoundException {
        for (String ceDevPath : CE_DEV_PATHS) {
            if (new File(ceDevPath).exists()) {
                return ceDevPath;
            }
        }
        throw new FileNotFoundException();
    }

    /**
     * Registers custom deserializers for {@link PermissionRequestForCreation} fields.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(ZonedDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                ZonedDateTime zonedDateTime = LocalDate.parse(text, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneOffset.UTC);
                setValue(zonedDateTime);
            }
        });

        binder.registerCustomEditor(PeriodResolutionEnum.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                PeriodResolutionEnum period = PeriodResolutionEnum.fromString(text);
                setValue(period);
            }
        });
    }

    @GetMapping(value = "/ce.js", produces = "text/javascript")
    public String javascriptConnectorElement() {
        try (InputStream in = getCEInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private InputStream getCEInputStream() throws FileNotFoundException {
        return !environment.matchesProfiles("dev")
                ? new FileInputStream(findCEDevPath())
                : Objects.requireNonNull(getClass().getResourceAsStream(CE_PRODUCTION_PATH));
    }

    @GetMapping(PERMISSION_STATUS_PATH + "/{permissionId}")
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@PathVariable String permissionId) {
        var statusMessage = service.findConnectionStatusMessageById(permissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find permission request"));

        return ResponseEntity.ok(statusMessage);
    }

    @PostMapping(value = "/permission-request", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> permissionRequest(@Valid @ModelAttribute PermissionRequestForCreation request) throws StateTransitionException {
        LOGGER.info("request was: {}", request);

        var permissionRequest = requestFactory.create(request);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();

        var response = ResponseEntity.ok("{\"permissionId\":\"" + permissionRequest.permissionId() + "\"}");

        permissionRequest.receivedPermissionAdministratorResponse();

        energinetCustomerApi.setRefreshToken(permissionRequest.refreshToken());
        energinetCustomerApi.setUserCorrelationId(UUID.fromString(permissionRequest.permissionId()));
        MeteringPoints meteringPoints = new MeteringPoints();
        meteringPoints.addMeteringPointItem(permissionRequest.meteringPoint());
        MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);
        permissionRequest.accept();


        fetchRecordsExecutor.execute(() -> {
            try {
                energinetCustomerApi.apiToken();
            } catch (FeignException e) {
                LOGGER.error("Something went wrong while fetching token from Energinet:", e);
            }

            try {
                var consumptionRecord = energinetCustomerApi.getTimeSeries(
                        permissionRequest.start(),
                        permissionRequest.end(),
                        permissionRequest.periodResolution(),
                        meteringPointsRequest
                );

                consumptionRecord.setConnectionId(permissionRequest.connectionId());
                consumptionRecord.setPermissionId(permissionRequest.permissionId());
                consumptionRecord.setDataNeedId(permissionRequest.dataNeedId());
                consumptionRecordSink.tryEmitNext(consumptionRecord);
            } catch (FeignException e) {
                LOGGER.error("Something went wrong while fetching data from Energinet:", e);
            }
        });

        return response;
    }
}
