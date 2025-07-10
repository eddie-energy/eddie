package energy.eddie.exampleappbackend.service;

import energy.eddie.cim.v0_82.vhd.PointComplexType;
import energy.eddie.cim.v0_82.vhd.SeriesPeriodComplexType;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
import energy.eddie.exampleappbackend.model.Permission;
import energy.eddie.exampleappbackend.model.TimeSeries;
import energy.eddie.exampleappbackend.model.TimeSeriesList;
import energy.eddie.exampleappbackend.persistence.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValidatedHistoricalDataService {
    // Runtime Cache in case the VHD Message is received before Permission (use more sophisticated cache for production environments)
    private final Map<String, ValidatedHistoricalDataEnvelope> cache = new LinkedHashMap<>();
    private final PermissionRepository permissionRepository;

    public void handleCachedMessageWithEddiePermissionId(String eddiePermissionId) {
        if (cache.containsKey(eddiePermissionId)) {
            handleCachedMessageWithEddiePermissionId(eddiePermissionId);
        }
    }

    public void handleValidatedHistoricalDataEnvelope(ValidatedHistoricalDataEnvelope vhdEnvelope) {
        var messageDocumentHeaderMetaInformation = vhdEnvelope.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        var eddiePermissionId = messageDocumentHeaderMetaInformation.getPermissionid();

        permissionRepository.findByEddiePermissionId(eddiePermissionId).ifPresentOrElse((permission) -> {
            if (permission.getTimeSeriesList() == null) {
                var timeSeriesList = createNewTimeSeriesListByValidatedHistoricalDataMarketDocument(vhdEnvelope.getValidatedHistoricalDataMarketDocument(), permission);
                permission.setTimeSeriesList(timeSeriesList);
                permissionRepository.save(permission);
                log.info("Created new Time Series List for permission with eddie permission id {}", eddiePermissionId);
            } else {
                var receivedTimeSeries = getTimeSeriesByValidatedHistoricalDataMarketDocument(vhdEnvelope.getValidatedHistoricalDataMarketDocument(), permission.getTimeSeriesList());
                if (!receivedTimeSeries.isEmpty()) {
                    var oldTimeSeries = permission.getTimeSeriesList().getTimeSeries();
                    var newTimeSeries = mergeTimeSeriesLists(oldTimeSeries, receivedTimeSeries);
                    permission.getTimeSeriesList().getTimeSeries().clear();
                    permission.getTimeSeriesList().getTimeSeries().addAll(newTimeSeries);
                    permissionRepository.save(permission);
                    log.info("Updated Time Series for Permission with EDDIE Permission ID {}!", eddiePermissionId);
                }
            }
        }, () -> {
            log.warn("Received Validated Historical Data for unknown permission with EDDIE Permission ID {}! Caching Message!", eddiePermissionId);
            cache.put(vhdEnvelope.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation().getPermissionid(), vhdEnvelope);
        });
    }

    private List<TimeSeries> mergeTimeSeriesLists(List<TimeSeries> existingTimeSeries, List<TimeSeries> newTimeSeriesList) {
        Map<Instant, TimeSeries> map = new LinkedHashMap<>();
        for (var timeSeries : existingTimeSeries) {
            map.put(timeSeries.getTimestamp(), timeSeries);
        }
        for (var timeSeries : newTimeSeriesList) {
            map.put(timeSeries.getTimestamp(), timeSeries); // overwrites old values
        }
        return new ArrayList<>(map.values());
    }

    private TimeSeriesList createNewTimeSeriesListByValidatedHistoricalDataMarketDocument(ValidatedHistoricalDataMarketDocumentComplexType vhdMd, Permission permission) {
        var timeSeriesList = TimeSeriesList.builder()
                .permission(permission)
                .build();
        var timeSeries = getTimeSeriesByValidatedHistoricalDataMarketDocument(vhdMd, timeSeriesList);
        timeSeriesList.setTimeSeries(timeSeries);
        return timeSeriesList;
    }

    private List<TimeSeries> getTimeSeriesByValidatedHistoricalDataMarketDocument(ValidatedHistoricalDataMarketDocumentComplexType vhdMd, TimeSeriesList timeSeriesList) {
        var result = new ArrayList<TimeSeries>();
        for (var cimTimeSeries : vhdMd.getTimeSeriesList().getTimeSeries()) {
            timeSeriesList.setUnitIfEmpty(cimTimeSeries);
            if (timeSeriesList.hasEqualUnitAs(cimTimeSeries)) {
                for (var timeSeriesPeriod : cimTimeSeries.getSeriesPeriodList().getSeriesPeriods()) {
                    timeSeriesList.setTemporalResolutionIfEmpty(timeSeriesPeriod);
                    if (timeSeriesList.hasEqualTemporalResolutionAs(timeSeriesPeriod)) {
                        result.addAll(getTimeSeriesFromTimePeriodPoints(timeSeriesPeriod.getPointList().getPoints(), timeSeriesPeriod, timeSeriesList));
                    } else {
                        log.warn("Received different resolutions for the same Permission! Ignoring Period with {} Resolution!", timeSeriesPeriod.getResolution());
                    }
                }
            } else {
                log.warn("Received different unit for the same Permission! Ignoring Time Series with {} Unit!", cimTimeSeries.getEnergyMeasurementUnitName().name());
            }
        }
        return result;
    }

    private List<TimeSeries> getTimeSeriesFromTimePeriodPoints(List<PointComplexType> timePeriodPoints, SeriesPeriodComplexType period, TimeSeriesList timeSeriesList) {
        var resolution = Duration.parse(period.getResolution());
        var start = OffsetDateTime.parse(period.getTimeInterval().getStart(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")).toInstant();
        return timePeriodPoints.stream()
                .map((point) -> {
                    int position = Integer.parseInt(point.getPosition());
                    var timestamp = start.plus(resolution.multipliedBy(position + 1));
                    var value = point.getEnergyQuantityQuantity().doubleValue();
                    return TimeSeries.builder()
                            .value(value)
                            .timestamp(timestamp)
                            .timeSeriesList(timeSeriesList)
                            .build();
                })
                .toList();
    }
}
