package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.ApiClient;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Reading;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Register;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.AccessTokenAndSingleSyncUrl;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthManager;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlInternalPollingEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.utils.LocalDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class PollingService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final Outbox outbox;
    private final OAuthManager oAuthManager;
    private final ApiClient apiClient;
    private final DataNeedsService dataNeedsService;

    private final Sinks.Many<IdentifiableMeteredData> identifiableMeteredDataSink = Sinks.many()
                                                                                         .multicast()
                                                                                         .onBackpressureBuffer();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    // DataNeedsService is injected from parent context
    public PollingService(
            Outbox outbox,
            OAuthManager oAuthManager,
            ApiClient apiClient,
            DataNeedsService dataNeedsService
    ) {
        this.outbox = outbox;
        this.oAuthManager = oAuthManager;
        this.apiClient = apiClient;
        this.dataNeedsService = dataNeedsService;
    }

    public void fetchConsumptionData(NlPermissionRequest permissionRequest) {
        String permissionId = permissionRequest.permissionId();
        LOGGER.debug("Fetching energy data for permission request {}", permissionId);
        AccessTokenAndSingleSyncUrl accessTokenAndSingleSyncUrl;
        try {
            accessTokenAndSingleSyncUrl = oAuthManager.accessTokenAndSingleSyncUrl(permissionId);
        } catch (OAuthTokenDetailsNotFoundException e) {
            LOGGER.error("Permission request {} does not have credentials", permissionId);
            return;
        } catch (JWTSignatureCreationException e) {
            LOGGER.error("Error creating signed JWT", e);
            return;
        } catch (OAuthUnavailableException e) {
            LOGGER.error("OAuth 2.0 Server was not reachable", e);
            return;
        } catch (IllegalTokenException | OAuthException | NoRefreshTokenException e) {
            LOGGER.info("Permission request {} was revoked by final customer", permissionId);
            outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
            return;
        }
        LOGGER.info("Fetching consumption data for permission request {}", permissionId);
        apiClient.fetchConsumptionData(accessTokenAndSingleSyncUrl.singleSync(),
                                       accessTokenAndSingleSyncUrl.accessToken())
                 .filter(meteredData -> !meteredData.isEmpty())
                 .map(meteredData -> new IdentifiableMeteredData(permissionRequest, meteredData))
                 .subscribe(this::consume);
    }

    private void consume(IdentifiableMeteredData identifiableMeteredData) {
        var permissionRequest = identifiableMeteredData.permissionRequest();
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Received energy data for permission request {}", permissionId);
        var response = filterEnergyType(identifiableMeteredData.meteredData(),
                                        permissionRequest.dataNeedId());
        if (response.isEmpty()) {
            return;
        }
        var lastMeterReadings = new HashMap<String, ZonedDateTime>();
        for (MijnAansluitingResponse mijnAansluitingResponse : response) {
            for (Register register : mijnAansluitingResponse.getMarketEvaluationPoint().getRegisterList()) {
                // Calculate deltas between readings, removes first reading day.
                register.setReadingList(delta(register.getReadingList()));

                // Update the last meter readings, even the ones that were already fulfilled.
                // That way we always have the latest information, for the fulfillment-handler
                var timestamp = register.getReadingList().getLast().getDateAndOrTime().getDateTime();
                String meteringPointId = register.getMeter().getMRID();
                lastMeterReadings.put(meteringPointId, timestamp);

                // Remove already received meter readings or before the start date from the response
                ZonedDateTime lastMeterReading = permissionRequest.lastMeterReadings().get(meteringPointId);
                var lastTimestamp = Optional.ofNullable(lastMeterReading)
                                            .orElse(permissionRequest.start().atStartOfDay(timestamp.getZone()));
                LOGGER.info("Removing all energy data before optional timestamp {} for permission request {}",
                            lastTimestamp,
                            permissionId);
                removeMeterReadingsBefore(register, lastTimestamp);

                // Remove the rest of the meter readings which are not part of the permission request timeframe
                // End of day calculation
                ZonedDateTime end = LocalDateUtils.endOfDay(permissionRequest.end(), timestamp.getZone());
                removeMeterReadingsAfter(register, end);
            }
        }
        outbox.commit(new NlInternalPollingEvent(permissionId, lastMeterReadings));
        identifiableMeteredDataSink.tryEmitNext(new IdentifiableMeteredData(permissionRequest, response));
    }

    private List<MijnAansluitingResponse> filterEnergyType(
            List<MijnAansluitingResponse> mijnAansluitingResponses,
            String dataNeedId
    ) {
        var dataNeed = dataNeedsService.findById(dataNeedId);
        if (dataNeed.isEmpty()) {
            LOGGER.error("DataNeed {} not found", dataNeedId);
            return List.of();
        }
        if (!(dataNeed.get() instanceof ValidatedHistoricalDataDataNeed validatedHistoricalDataDataNeed)) {
            LOGGER.error("Invalid type for of DataNeed {}", dataNeed);
            return List.of();
        }
        /*
          The metering point ID starts with a G for Gas and with an E for Electricity.
          This region connector does not support any other energy types right now.
         */
        var energyType = validatedHistoricalDataDataNeed.energyType() == EnergyType.NATURAL_GAS ? "G" : "E";
        var newResponseList = new ArrayList<MijnAansluitingResponse>();
        for (MijnAansluitingResponse mijnAansluitingResponse : mijnAansluitingResponses) {
            var newRegisterList = new ArrayList<Register>();
            for (Register register : mijnAansluitingResponse.getMarketEvaluationPoint().getRegisterList()) {
                if (isEnergyType(register, energyType)) {
                    newRegisterList.add(register);
                }
            }
            if (!newRegisterList.isEmpty()) {
                mijnAansluitingResponse.getMarketEvaluationPoint()
                                       .setRegisterList(newRegisterList);
                newResponseList.add(mijnAansluitingResponse);
            }
        }
        return newResponseList;
    }

    /**
     * Gets a reading list and will calculate the deltas between consecutive items in the list. By calculating deltas
     * the first item will be dropped.
     *
     * @param readings list of readings with total cumulative values.
     * @return list of readings, with the delta as reading values. It has the size of the original list - 1.
     */
    private static List<Reading> delta(List<Reading> readings) {
        var array = readings.toArray(new Reading[0]);
        List<Reading> deltas = new ArrayList<>(array.length - 1);
        for (int i = array.length - 1; i > 0; i--) {
            var prev = array[i - 1];
            var current = array[i];
            var delta = current.getValue().subtract(prev.getValue());
            current.setValue(delta);
            deltas.addFirst(current);
        }
        return deltas;
    }

    private static void removeMeterReadingsBefore(Register register, ZonedDateTime lastTimestamp) {
        removeMeterReadings(register, lastTimestamp::isBefore);
    }

    private static void removeMeterReadingsAfter(Register register, ZonedDateTime lastTimestamp) {
        removeMeterReadings(register, lastTimestamp::isAfter);
    }

    private static boolean isEnergyType(Register register, String energyType) {
        return register.getMeter()
                       .getMRID()
                       .startsWith(energyType);
    }

    private static void removeMeterReadings(Register register, Predicate<ZonedDateTime> func) {
        List<Reading> list = new ArrayList<>();
        for (Reading reading : register.getReadingList()) {
            var dateTime = reading.getDateAndOrTime().getDateTime();
            if (func.test(dateTime)) {
                list.add(reading);
            }
        }
        LOGGER.atInfo()
              .addArgument(list::size)
              .addArgument(() -> register.getReadingList().size())
              .log("Filtered meter readings resulting in a list of {} out of {} initial records");

        register.setReadingList(list);
    }

    public Flux<IdentifiableMeteredData> identifiableMeteredDataFlux() {
        return identifiableMeteredDataSink.asFlux();
    }

    @Override
    public void close() {
        identifiableMeteredDataSink.tryEmitComplete();
    }
}
