package energy.eddie.core.dataneeds;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * REST controller for DataNeeds, limited to getting single data needs.
 * <ul>
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
        return ResponseEntity.ok(dataNeedsService.getDataNeeds());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataNeed> getDataNeeds(@PathVariable String id) {
        return ResponseEntity.of(dataNeedsService.getDataNeed(id));
    }

    @GetMapping("/types")
    public ResponseEntity<Set<String>> getDataTypes() {
        return ResponseEntity.ok(dataNeedsService.getDataNeedTypes());
    }

    @GetMapping("/granularities")
    public ResponseEntity<Set<String>> getDataGranularities() {
        return ResponseEntity.ok(dataNeedsService.getDataNeedGranularities());
    }
}
