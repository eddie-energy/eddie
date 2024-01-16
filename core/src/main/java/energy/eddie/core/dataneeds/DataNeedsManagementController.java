package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import energy.eddie.core.dataneeds.exceptions.DataNeedAlreadyExistsException;
import energy.eddie.core.dataneeds.exceptions.DataNeedIdsNotMatchingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller for managing data needs that are stored in the application database. This provides
 * common CRUD operations for data needs as documented in the APIS.md file.
 */
@RestController
@RequestMapping(path = DataNeedsManagementController.BASE_PATH_KEY, produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public class DataNeedsManagementController {
    public static final String BASE_PATH_KEY = "${management.server.urlprefix}/data-needs";
    public final String basePath;
    private final DataNeedsDbRepository dataNeedsDbRepository;

    public DataNeedsManagementController(DataNeedsDbRepository dataNeedsDbRepository, @Value(BASE_PATH_KEY) String basePath) {
        this.dataNeedsDbRepository = dataNeedsDbRepository;
        this.basePath = basePath;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createDataNeed(@NonNull @RequestBody DataNeedEntity newDataNeed) throws DataNeedAlreadyExistsException {
        final var id = newDataNeed.id();
        if (dataNeedsDbRepository.existsById(id)) {
            throw new DataNeedAlreadyExistsException(id);
        }
        dataNeedsDbRepository.save(newDataNeed);
        return ResponseEntity.created(URI.create(basePath + "/" + id)).build();
    }

    @GetMapping()
    public ResponseEntity<Iterable<DataNeed>> getAllDataNeeds() {
        return ResponseEntity.ok(dataNeedsDbRepository.findAll().stream().map(DataNeed.class::cast).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataNeed> getDataNeed(@PathVariable String id) throws DataNeedNotFoundException {
        return dataNeedsDbRepository.findById(id)
                .<ResponseEntity<DataNeed>>map(ResponseEntity::ok)
                .orElseThrow(() -> new DataNeedNotFoundException(id, false));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateDataNeed(@PathVariable String id, @RequestBody DataNeedEntity dataNeed) throws DataNeedNotFoundException, DataNeedIdsNotMatchingException {
        if (!dataNeed.id().equals(id)) {
            throw new DataNeedIdsNotMatchingException();
        } else if (!dataNeedsDbRepository.existsById(id)) {
            throw new DataNeedNotFoundException(id, false);
        } else {
            dataNeedsDbRepository.save(dataNeed);
            return ResponseEntity.ok().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDataNeed(@PathVariable String id) throws DataNeedNotFoundException {
        if (!dataNeedsDbRepository.existsById(id)) {
            throw new DataNeedNotFoundException(id, false);
        }
        dataNeedsDbRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}