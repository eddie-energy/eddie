package energy.eddie.framework.dataneeds;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for DataNeeds, limited to getting single data needs.
 * <ul>
 *     <li>GET /api/data-needs/{id} : retrieve a data need by it's id</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/data-needs")
public class DataNeedsController {

    private DataNeedsService dataNeedsService;

    public DataNeedsController(DataNeedsService dataNeedsConfigService) {
        this.dataNeedsService = dataNeedsConfigService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataNeed> getDataNeeds(@PathVariable String id) {
        return ResponseEntity.of(dataNeedsService.getDataNeed(id));
    }
}
