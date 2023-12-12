package energy.eddie.core.dataneeds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(DataNeedsManagementController.class);

    private final DataNeedsDbRepository dataNeedsDbRepository;

    private final Validator validator;
    private final ObjectMapper jsonMapper;

    public DataNeedsManagementController(DataNeedsDbRepository dataNeedsDbRepository, Validator validator, ObjectMapper jsonMapper) {
        this.dataNeedsDbRepository = dataNeedsDbRepository;
        this.validator = validator;
        this.jsonMapper = jsonMapper;
    }

    private class DataNeedValidator {
        private final List<String> violations;

        public DataNeedValidator(DataNeed dataNeed) {
            this.violations = dataNeed.validate(validator);
        }

        public boolean isValid() {
            return violations.isEmpty();
        }

        public ResponseEntity<String> getErrorResponse() {
            if (isValid()) {
                throw new IllegalStateException("data need is valid");
            } else {
                String json = null;
                try {
                    json = jsonMapper.writeValueAsString(violations);
                } catch (JsonProcessingException e) {
                    LOGGER.error("failed to serialize validation errors", e);
                }
                return ResponseEntity.badRequest().body(json);
            }
        }
    }


    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<String> createDataNeed(@NotNull @RequestBody DataNeed newDataNeed) {
        final var id = newDataNeed.getId();
        final var dataNeedValidator = new DataNeedValidator(newDataNeed);
        if (!dataNeedValidator.isValid()) {
            return dataNeedValidator.getErrorResponse();
        }
        if (dataNeedsDbRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("data need with id " + id + " already exists");
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
    public ResponseEntity<String> updateDataNeed(@PathVariable String id, @RequestBody DataNeed dataNeed) {
        final var dataNeedValidator = new DataNeedValidator(dataNeed);
        if (!dataNeedValidator.isValid()) {
            return dataNeedValidator.getErrorResponse();
        }
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
