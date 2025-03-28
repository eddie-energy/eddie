package energy.eddie.regionconnector.at.eda.provider;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.projections.MeterReadingTimeframe;
import energy.eddie.regionconnector.at.eda.persistence.MeterReadingTimeframeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@Component
@ConditionalOnProperty(name = "region-connector.at.eda.consumption.records.remove.duplicates.enabled", havingValue = "true")
public class DuplicateDataFilter implements UnaryOperator<IdentifiableConsumptionRecord> {
    private final MeterReadingTimeframeRepository repository;

    public DuplicateDataFilter(MeterReadingTimeframeRepository repository) {this.repository = repository;}

    @Override
    public IdentifiableConsumptionRecord apply(IdentifiableConsumptionRecord consumptionRecord) {
        List<AtPermissionRequest> filteredRequests = new ArrayList<>();
        for (var permissionRequest : consumptionRecord.permissionRequests()) {
            var readings = repository.findAllByPermissionId(permissionRequest.permissionId());
            var notContained = readings.stream()
                                       .noneMatch(reading -> isContained(consumptionRecord, reading));
            if (notContained) {
                filteredRequests.add(permissionRequest);
            }
        }
        return new IdentifiableConsumptionRecord(
                consumptionRecord.consumptionRecord(),
                filteredRequests,
                consumptionRecord.meterReadingStartDate(),
                consumptionRecord.meterReadingEndDate()
        );
    }

    private boolean isContained(IdentifiableConsumptionRecord request, MeterReadingTimeframe reading) {
        return isBeforeOrEquals(reading.start(),
                                request.meterReadingStartDate()) && isBeforeOrEquals(request.meterReadingEndDate(),
                                                                                     reading.end());
    }

    private boolean isBeforeOrEquals(LocalDate left, LocalDate right) {
        return left.isBefore(right) || left.isEqual(right);
    }
}
