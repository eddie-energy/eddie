package energy.eddie.exampleappbackend.service;

import energy.eddie.cim.v1_04.QuantityTypeKind;
import energy.eddie.cim.v1_04.RTDEnvelope;
import energy.eddie.exampleappbackend.model.TimeSeries;
import energy.eddie.exampleappbackend.model.TimeSeriesList;
import energy.eddie.exampleappbackend.persistence.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealTimeDataService {
    private final PermissionRepository permissionRepository;

    @Transactional
    public void handelRealTimeDataEnvelope(RTDEnvelope rtdEnvelope) {
        var eddiePermissionId = rtdEnvelope.getMessageDocumentHeaderMetaInformationPermissionId();
        permissionRepository.findByEddiePermissionId(eddiePermissionId).ifPresentOrElse((permission) -> {
            if (permission.getTimeSeriesList() == null) {
                var timeSeriesList = TimeSeriesList.builder()
                        .temporalResolution("n/a")
                        .unit("KILOWATT_HOUR")
                        .permission(permission)
                        .build();
                timeSeriesList.setTimeSeries(getTimeSeriesFromRTDEnvelope(rtdEnvelope, timeSeriesList));
                permission.setTimeSeriesList(timeSeriesList);
                permissionRepository.save(permission);
                log.info("Created new Time Series List for permission with EDDIE permission id {}", eddiePermissionId);
            } else {
                permission.getTimeSeriesList().getTimeSeries().addAll(getTimeSeriesFromRTDEnvelope(rtdEnvelope, permission.getTimeSeriesList()));
                permissionRepository.save(permission);
                log.info("Added new Time Series for permission with EDDIE permission id {}", eddiePermissionId);
            }
        }, () -> {
            log.info("Received message for EDDIE permission id {}, which is not tracked! Ignoring Message!", eddiePermissionId);
        });

    }

    private List<TimeSeries> getTimeSeriesFromRTDEnvelope(RTDEnvelope rtdEnvelope, TimeSeriesList timeSeriesList) {
        var result = new ArrayList<TimeSeries>();
        for (var cimTimeSeries : rtdEnvelope.getMarketDocument().getTimeSeries()) {
            for (var quantity : cimTimeSeries.getQuantities()) {
                if (quantity.getType().equals(QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH)) {
                    result.add(TimeSeries.builder()
                            .value(quantity.getQuantity().doubleValue())
                            .timestamp(cimTimeSeries.getDateAndOrTimeDateTime().toInstant())
                            .timeSeriesList(timeSeriesList)
                            .build()
                    );
                }
            }
        }
        return result;
    }
}
