package energy.eddie.framework.dataneeds;

import energy.eddie.api.v0.ConsumptionRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataNeedsController {

    @GetMapping("/api/data-needs/{id}")
    public DataNeed getDataNeeds(@PathVariable String id) {
        return new DataNeed("description: " + id, DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA, ConsumptionRecord.MeteringInterval.P_1_D, -180, false, 0);
    }
}
