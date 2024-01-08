package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.DataNeed;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static energy.eddie.core.dataneeds.DataNeedsManagementController.URL_PREFIX;

/**
 * REST controller for managing data needs that are stored in the application database. This provides
 * common CRUD operations for data needs as documented in the APIS.md file.
 */
@RestController
@RequestMapping(path = URL_PREFIX, produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public class DataNeedsManagementController {
    public static final String URL_PREFIX = "/management/data-needs";

    private final DataNeedsDbRepository dataNeedsDbRepository;

    public DataNeedsManagementController(DataNeedsDbRepository dataNeedsDbRepository) {
        this.dataNeedsDbRepository = dataNeedsDbRepository;
    }

    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createDataNeed(@NonNull @RequestBody DataNeedEntity newDataNeed) {
        final var id = newDataNeed.id();
        if (dataNeedsDbRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("data need with id " + id + " already exists");
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
        return dataNeedsDbRepository.findById(id)
                .<ResponseEntity<DataNeed>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(path = "/{id}", produces = MediaType.TEXT_PLAIN_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateDataNeed(@PathVariable String id, @RequestBody DataNeedEntity dataNeed) {
        if (!dataNeed.id().equals(id)) {
            return ResponseEntity.badRequest().body("data need id in url does not match data need id in body");
        } else if (!dataNeedsDbRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        } else {
            dataNeedsDbRepository.save(dataNeed);
            return ResponseEntity.ok().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataNeed(@PathVariable String id) {
        if (!dataNeedsDbRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dataNeedsDbRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}