package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.DataNeed;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller for managing data needs that are stored in the application database. This provides
 * common CRUD operations for data needs as documented in the APIS.md file.
 */
@RestController
@RequestMapping("/management/data-needs")
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public class DataNeedsManagementController {
    public static final String URL_PREFIX = "/management/data-needs";

    private final DataNeedsDbRepository dataNeedsDbRepository;

    public DataNeedsManagementController(DataNeedsDbRepository dataNeedsDbRepository) {
        this.dataNeedsDbRepository = dataNeedsDbRepository;
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<String> createDataNeed(@NonNull @RequestBody DataNeedImpl newDataNeed) {
        final var id = newDataNeed.getId();
        if (dataNeedsDbRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("data need with id " + id + " already exists");
        }
        dataNeedsDbRepository.save(newDataNeed);
        return ResponseEntity.created(URI.create(URL_PREFIX + "/" + id)).build();
    }

    @GetMapping()
    public ResponseEntity<Iterable<DataNeed>> getAllDataNeeds() {
        return ResponseEntity.ok(dataNeedsDbRepository.findAll().stream().map(DataNeed.class::cast).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataNeed> getDataNeed(@PathVariable String id) {
        final var dataNeed = dataNeedsDbRepository.findById(id);
        return dataNeed.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(dataNeed.get());
    }

    @PostMapping("/{id}")
    public ResponseEntity<String> updateDataNeed(@PathVariable String id, @RequestBody DataNeedImpl dataNeed) {
        if (!dataNeed.getId().equals(id)) {
            return ResponseEntity.badRequest().body("data need id in url does not match data need id in body");
        } else if (!dataNeedsDbRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        } else {
            dataNeedsDbRepository.save(dataNeed);
            return ResponseEntity.ok().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteDataNeed(@PathVariable String id) {
        dataNeedsDbRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
