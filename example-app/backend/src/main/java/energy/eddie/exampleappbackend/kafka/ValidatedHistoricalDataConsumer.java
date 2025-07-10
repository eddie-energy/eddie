package energy.eddie.exampleappbackend.kafka;

import energy.eddie.cim.v0_82.vhd.PointComplexType;
import energy.eddie.cim.v0_82.vhd.SeriesPeriodComplexType;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
import energy.eddie.exampleappbackend.model.Permission;
import energy.eddie.exampleappbackend.model.TimeSeries;
import energy.eddie.exampleappbackend.model.TimeSeriesList;
import energy.eddie.exampleappbackend.persistence.PermissionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
public class ValidatedHistoricalDataConsumer {
    private final PermissionRepository permissionRepository;

    @Transactional
    @KafkaListener(topics = "ep.eddie-demo-hardening.cim_0_82.validated-historical-data-md", containerFactory = "validatedHistoricalDataEnvelopeListenerContainerFactory")
    public void listen(ConsumerRecord<String, ValidatedHistoricalDataEnvelope> consumerRecord) {
        log.info("Received a new Validated Historical Data Message! Processing ...");
        if (consumerRecord.value() == null) {
            log.warn("Validated Historical Data Envelope is empty! Ignoring Message!");
            return;
        }
        var vhdEnvelope = consumerRecord.value();
        var messageDocumentHeaderMetaInformation = vhdEnvelope.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        var eddiePermissionId = messageDocumentHeaderMetaInformation.getPermissionid();

        var optionalPermission = permissionRepository.findByEddiePermissionId(eddiePermissionId);
        if (optionalPermission.isPresent()) {
            var permission = optionalPermission.get();
            if (permission.getTimeSeriesList() == null) {
                var timeSeriesList = createTimeSeriesListForValidatedHistoricalDataMarketDocument(vhdEnvelope.getValidatedHistoricalDataMarketDocument(), permission);
                permission.setTimeSeriesList(timeSeriesList);
                permissionRepository.save(permission);
                log.info("Created new Time Series List for permission with eddie permission id {}", eddiePermissionId);
            } else {
                var receivedTimeSeries = getTimeSeriesFromValidatedHistoricalDataMarketDocument(vhdEnvelope.getValidatedHistoricalDataMarketDocument(), permission.getTimeSeriesList());
                var oldTimeSeries = permission.getTimeSeriesList().getTimeSeries();
                var newTimeSeries = mergeTimeSeriesLists(oldTimeSeries, receivedTimeSeries);
                permission.getTimeSeriesList().getTimeSeries().clear();
                permission.getTimeSeriesList().getTimeSeries().addAll(newTimeSeries);
                permissionRepository.save(permission);
                log.info("Updated Time Series for Permission with EDDIE Permission ID {}!", eddiePermissionId);
            }
        } else {
            log.warn("Received Validated Historical Data for unknown permission with EDDIE Permission ID {}! Ignoring Message!", eddiePermissionId);
        }
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

    private TimeSeriesList createTimeSeriesListForValidatedHistoricalDataMarketDocument(ValidatedHistoricalDataMarketDocumentComplexType vhdMd, Permission permission) {
        var timeSeriesList = TimeSeriesList.builder()
                .permission(permission)
                .build();
        var timeSeries = getTimeSeriesFromValidatedHistoricalDataMarketDocument(vhdMd, timeSeriesList);
        timeSeriesList.setTimeSeries(timeSeries);
        return timeSeriesList;
    }

    private List<TimeSeries> getTimeSeriesFromValidatedHistoricalDataMarketDocument(ValidatedHistoricalDataMarketDocumentComplexType vhdMd, TimeSeriesList timeSeriesList) {
        var result = new ArrayList<TimeSeries>();
        for (var cimTimeSeries : vhdMd.getTimeSeriesList().getTimeSeries()) {
            if (timeSeriesList.getUnit() == null) {
                timeSeriesList.setUnit(cimTimeSeries.getEnergyMeasurementUnitName().name());
            }
            if (timeSeriesList.getUnit().equals(cimTimeSeries.getEnergyMeasurementUnitName().name())) {
                for (var timeSeriesPeriod : cimTimeSeries.getSeriesPeriodList().getSeriesPeriods()) {
                    if (timeSeriesList.getTemporalResolution() == null) {
                        timeSeriesList.setTemporalResolution(timeSeriesPeriod.getResolution());
                    }
                    if (timeSeriesList.getTemporalResolution().equals(timeSeriesPeriod.getResolution())) {
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
