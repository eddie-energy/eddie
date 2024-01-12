package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.api.agnostic.DataType;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * REST controller for retrieving DataNeeds and their possible values.
 * <ul>
 *     <li>GET /api/data-needs : retrieve just the ids of all data needs</li>
 *     <li>GET /api/data-needs/types : get all possible values for the type field of a data need</li>
 *     <li>GET /api/data-needs/granularities : get all possible values for the granularity field of a data need</li>
 *     <li>GET /api/data-needs/{id} : retrieve a data need by it's id</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/data-needs")
public class DataNeedsController {

    private final DataNeedsService dataNeedsService;

    public DataNeedsController(DataNeedsService dataNeedsConfigService) {
        this.dataNeedsService = dataNeedsConfigService;
    }

    @GetMapping()
    public ResponseEntity<Set<String>> getDataNeeds() {
        return ResponseEntity.ok(dataNeedsService.getAllDataNeedIds());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataNeed> getDataNeeds(@PathVariable String id) throws DataNeedNotFoundException {
        var dataNeed = dataNeedsService.getDataNeed(id).orElseThrow(() -> new DataNeedNotFoundException(id, false));
        return ResponseEntity.ok(dataNeed);
    }

    @GetMapping("/types")
    public ResponseEntity<DataType[]> getDataTypes() {
        return ResponseEntity.ok(DataType.values());
    }

    @GetMapping("/granularities")
    public ResponseEntity<Granularity[]> getDataGranularities() {
        return ResponseEntity.ok(Granularity.values());
    }
}