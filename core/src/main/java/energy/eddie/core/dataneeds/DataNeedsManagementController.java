package energy.eddie.core.dataneeds;

import jakarta.validation.Validator;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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

    private final Validator validator;

    public DataNeedsManagementController(DataNeedsDbRepository dataNeedsDbRepository, Validator validator) {
        this.dataNeedsDbRepository = dataNeedsDbRepository;
        this.validator = validator;
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<List<String>> createDataNeed(@NotNull @RequestBody DataNeed newDataNeed) {
        final var id = newDataNeed.getId();
        final var violations = newDataNeed.validate(validator);
        if (!violations.isEmpty()) {
            return ResponseEntity.badRequest().body(violations);
        }
        if (dataNeedsDbRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(List.of("data need with id " + id + " already exists"));
        }
        dataNeedsDbRepository.save(newDataNeed);
        return ResponseEntity.created(URI.create(URL_PREFIX + "/" + id)).build();
    }

    @GetMapping()
    public ResponseEntity<Iterable<DataNeed>> getAllDataNeeds() {
        return ResponseEntity.ok(dataNeedsDbRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataNeed> getDataNeed(@PathVariable String id) {
        final var dataNeed = dataNeedsDbRepository.findById(id);
        return dataNeed.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(dataNeed.get());
    }

    @PostMapping("/{id}")
    public ResponseEntity<List<String>> updateDataNeed(@PathVariable String id, @RequestBody DataNeed dataNeed) {
        final var violations = dataNeed.validate(validator);
        if (!violations.isEmpty()) {
            return ResponseEntity.badRequest().body(violations);
        }
        if (!dataNeed.getId().equals(id)) {
            return ResponseEntity.badRequest().body(List.of("data need id in url does not match data need id in body"));
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
