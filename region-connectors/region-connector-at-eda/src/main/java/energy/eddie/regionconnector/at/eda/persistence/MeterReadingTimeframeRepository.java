package energy.eddie.regionconnector.at.eda.persistence;


import energy.eddie.regionconnector.at.eda.permission.request.projections.MeterReadingTimeframe;
import org.springframework.data.repository.Repository;

import java.util.List;

@org.springframework.stereotype.Repository
public interface MeterReadingTimeframeRepository extends Repository<MeterReadingTimeframe, Long> {
    List<MeterReadingTimeframe> findAllByPermissionId(String permissionId);
}
