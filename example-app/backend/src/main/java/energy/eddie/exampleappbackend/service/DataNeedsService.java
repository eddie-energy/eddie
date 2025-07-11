package energy.eddie.exampleappbackend.service;

import energy.eddie.data_needs.generated.api.DataNeedsApiClient;
import energy.eddie.exampleappbackend.exception.FailedToFetchDataNeedException;
import energy.eddie.exampleappbackend.model.db.PermissionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DataNeedsService {
    private final DataNeedsApiClient dataNeedsApiClient;

    public PermissionType getPermissionTypeByDataNeedId(String eddieDataNeedId) {
        var response = dataNeedsApiClient.getDataNeed(eddieDataNeedId);
        if (response.getStatusCode().is2xxSuccessful()) {
            var dataNeed = response.getBody();

            if (dataNeed != null && dataNeed.getType() != null && dataNeed.getType().equals("validated")) {
                return PermissionType.VALIDATED_HISTORICAL_DATA;
            } else if (dataNeed != null && dataNeed.getType() != null && dataNeed.getType().equals("aiida")) {
                return PermissionType.REAL_TIME_DATA;
            } else {
                return PermissionType.OTHER;
            }
        } else {
            throw new FailedToFetchDataNeedException();
        }
    }
}
