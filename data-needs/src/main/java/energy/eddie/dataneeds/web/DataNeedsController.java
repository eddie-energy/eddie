package energy.eddie.dataneeds.web;

import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/data-needs")
public class DataNeedsController {
    private final DataNeedsService dataNeedsService;

    public DataNeedsController(DataNeedsService dataNeedsService) {
        this.dataNeedsService = dataNeedsService;
    }

    @GetMapping
    public ResponseEntity<List<DataNeedsNameAndIdProjection>> getDataNeedIdsAndNames() {
        return ResponseEntity.ok(dataNeedsService.getDataNeedIdsAndNames());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataNeed> getDataNeed(@PathVariable String id) throws DataNeedNotFoundException {
        return dataNeedsService.findById(id)
                               .map(ResponseEntity::ok)
                               .orElseThrow(() -> new DataNeedNotFoundException(id, false));
    }
}
