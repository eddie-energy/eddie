package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.ApiClient;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.CodeboekApiClient;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoints;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Reading;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Register;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.AccessTokenAndSingleSyncUrl;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthManager;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlInternalPollingEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.tasks.AccountingPointFilterTask;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;

import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.NL_ZONE_ID;
import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

@Service
public class PollingService implements AutoCloseable, CommonPollingService<MijnAansluitingPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final Outbox outbox;
    private final OAuthManager oAuthManager;
    private final ApiClient apiClient;
    private final DataNeedsService dataNeedsService;
    private final CodeboekApiClient codeboekApiClient;

    private final Sinks.Many<IdentifiableMeteredData> identifiableMeteredDataSink = Sinks.many()
                                                                                         .multicast()
                                                                                         .onBackpressureBuffer();
    private final Sinks.Many<IdentifiableAccountingPointData> identifiableAccountingPointDataSink = Sinks.many()
                                                                                                         .multicast()
                                                                                                         .onBackpressureBuffer();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    // DataNeedsService is injected from parent context
    public PollingService(
            Outbox outbox,
            OAuthManager oAuthManager,
            ApiClient apiClient,
            DataNeedsService dataNeedsService,
            CodeboekApiClient codeboekApiClient
    ) {
        this.outbox = outbox;
        this.oAuthManager = oAuthManager;
        this.apiClient = apiClient;
        this.dataNeedsService = dataNeedsService;
        this.codeboekApiClient = codeboekApiClient;
    }

    public void fetchAccountingPointData(MijnAansluitingPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        if (permissionRequest.postalCode() == null || permissionRequest.houseNumber() == null) {
            LOGGER.warn("No postal code or house number found for permission id: {}", permissionId);
            outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
            return;
        }
        var res = fetchAccessToken(permissionId);
        if (res.isEmpty()) {
            return;
        }
        LOGGER.info("Fetching accounting point data for permission request {}", permissionId);
        Mono.zip(
                    apiClient.fetchConsumptionData(res.get().singleSync(), res.get().accessToken()),
                    codeboekApiClient.meteringPoints(permissionRequest.postalCode(), permissionRequest.houseNumber())
                                     .flatMapIterable(MeteringPoints::getMeteringPoints)
                                     .collectList()
            )
            .map(new AccountingPointFilterTask())
            .map(apData -> new IdentifiableAccountingPointData(permissionRequest, apData.getT2()))
            .subscribe(this::consume);
    }

    @Override
    public void pollTimeSeriesData(MijnAansluitingPermissionRequest permissionRequest) {
        var start = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());
        var today = LocalDate.now(NL_ZONE_ID);
        var end = permissionRequest.end().isAfter(today) ? today : permissionRequest.end();
        pollTimeSeriesData(permissionRequest, start, end)
                .subscribe(res -> outbox.commit(new NlInternalPollingEvent(permissionRequest.permissionId(), res)));
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(MijnAansluitingPermissionRequest permissionRequest) {
        var today = LocalDate.now(NL_ZONE_ID);
        if (permissionRequest.status() != PermissionProcessStatus.ACCEPTED
            || !permissionRequest.start().isBefore(today)) {
            return false;
        }
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeed = dataNeedsService.getById(dataNeedId);
        return dataNeed instanceof ValidatedHistoricalDataDataNeed;
    }

    public Mono<Map<String, ZonedDateTime>> pollTimeSeriesData(
            MijnAansluitingPermissionRequest permissionRequest,
            LocalDate start,
            LocalDate end
    ) {
        String permissionId = permissionRequest.permissionId();
        var res = fetchAccessToken(permissionId);
        if (res.isEmpty()) {
            return Mono.empty();
        }
        var accessTokenAndSingleSyncUrl = res.get();
        LOGGER.info("Fetching consumption data for permission request {}", permissionId);
        return apiClient.fetchConsumptionData(accessTokenAndSingleSyncUrl.singleSync(),
                                              accessTokenAndSingleSyncUrl.accessToken())
                        .map(meteredData -> new IdentifiableMeteredData(permissionRequest, meteredData))
                        .map(readings -> consume(readings, start, end))
                        .filter(readings -> !readings.isEmpty());
    }

    public Flux<IdentifiableMeteredData> identifiableMeteredDataFlux() {
        return identifiableMeteredDataSink.asFlux();
    }

    public Flux<IdentifiableAccountingPointData> identifiableAccountingPointDataFlux() {
        return identifiableAccountingPointDataSink.asFlux();
    }

    @Override
    public void close() {
        identifiableMeteredDataSink.tryEmitComplete();
        identifiableAccountingPointDataSink.tryEmitComplete();
    }

    private Optional<AccessTokenAndSingleSyncUrl> fetchAccessToken(String permissionId) {
        try {
            return Optional.of(oAuthManager.accessTokenAndSingleSyncUrl(permissionId));
        } catch (OAuthTokenDetailsNotFoundException e) {
            LOGGER.error("Permission request {} does not have credentials", permissionId);
            return Optional.empty();
        } catch (JWTSignatureCreationException e) {
            LOGGER.error("Error creating signed JWT", e);
            return Optional.empty();
        } catch (OAuthUnavailableException e) {
            LOGGER.error("OAuth 2.0 Server was not reachable", e);
            return Optional.empty();
        } catch (IllegalTokenException | OAuthException | NoRefreshTokenException e) {
            LOGGER.info("Permission request {} was revoked by final customer", permissionId);
            outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
            return Optional.empty();
        }
    }

    private void consume(IdentifiableAccountingPointData identifiableAccountingPointData) {
        var permissionId = identifiableAccountingPointData.permissionRequest().permissionId();
        if (identifiableAccountingPointData.payload().isEmpty()) {
            LOGGER.info("No metering points found for permission request {}", permissionId);
            outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
            return;
        }
        identifiableAccountingPointDataSink.tryEmitNext(identifiableAccountingPointData);
        var event = new NlSimpleEvent(permissionId, PermissionProcessStatus.FULFILLED);
        outbox.commit(event);
    }

    private Map<String, ZonedDateTime> consume(
            IdentifiableMeteredData identifiableMeteredData,
            LocalDate start,
            LocalDate end
    ) {
        var permissionRequest = identifiableMeteredData.permissionRequest();
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Received energy data for permission request {}", permissionId);
        var response = filterEnergyType(identifiableMeteredData.meteredData(), permissionRequest.dataNeedId());
        if (response.isEmpty()) {
            LOGGER.info("Received empty response for energy data for permission request {}", permissionId);
            return Map.of();
        }
        var lastMeterReadings = new HashMap<String, ZonedDateTime>();
        for (MijnAansluitingResponse mijnAansluitingResponse : response) {
            for (Register register : mijnAansluitingResponse.getMarketEvaluationPoint().getRegisterList()) {

                // Update the last meter readings, even the ones that were already fulfilled.
                // That way we always have the latest information, for the fulfillment-handler
                var timestamp = register.getReadingList().getLast().getDateAndOrTime().getDateTime();
                String meteringPointId = register.getMeter().getMRID();
                lastMeterReadings.put(meteringPointId, timestamp);

                // Remove already received meter readings or before the start date from the response
                var lastTimestamp = start.atStartOfDay(timestamp.getZone());
                LOGGER.info("Removing all energy data before optional timestamp {} for permission request {}",
                            lastTimestamp,
                            permissionId);
                removeMeterReadingsBefore(register, lastTimestamp);

                // Remove the rest of the meter readings which are not part of the permission request timeframe
                LOGGER.info("Removing all energy data after optional timestamp {} for permission request {}",
                            end,
                            permissionId);
                removeMeterReadingsAfter(register, endOfDay(end, timestamp.getZone()));
            }
        }
        identifiableMeteredDataSink.tryEmitNext(new IdentifiableMeteredData(permissionRequest, response));
        return lastMeterReadings;
    }

    private static void removeMeterReadingsBefore(Register register, ZonedDateTime lastTimestamp) {
        removeMeterReadings(register, lastTimestamp::isBefore);
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

    private static void removeMeterReadingsAfter(Register register, ZonedDateTime lastTimestamp) {
        removeMeterReadings(register, lastTimestamp::isAfter);
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

    private static boolean isEnergyType(Register register, String energyType) {
        return register.getMeter()
                       .getMRID()
                       .startsWith(energyType);
    }
}
