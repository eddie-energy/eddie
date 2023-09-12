package energy.eddie.framework.dataneeds;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;

@RestController
public class DataNeedsController {

    private DataNeedsService dataNeedsService;

    public DataNeedsController(DataNeedsService dataNeedsService) {
        this.dataNeedsService = dataNeedsService;
    }

    @Nullable
    @GetMapping("/api/data-needs/{id}")
    public DataNeed getDataNeeds(@PathVariable String id) {
        return dataNeedsService.getDataNeed(id);
    }
}
