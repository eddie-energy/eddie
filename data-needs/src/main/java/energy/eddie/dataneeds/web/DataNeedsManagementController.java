package energy.eddie.dataneeds.web;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsDbService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping(path = DataNeedsManagementController.BASE_PATH_KEY, produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public class DataNeedsManagementController {
    public static final String BASE_PATH_KEY = "${management.server.urlprefix}/data-needs";
    private final DataNeedsDbService service;
    public final String basePath;

    public DataNeedsManagementController(DataNeedsDbService service, @Value(BASE_PATH_KEY) String basePath) {
        this.service = service;
        this.basePath = basePath;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataNeed> createNewDataNeed(@Validated @RequestBody DataNeed newDataNeed) {
        DataNeed savedDataNeed = service.saveNewDataNeed(newDataNeed);

        return ResponseEntity
                .created(URI.create(basePath + "/" + newDataNeed.id()))
                .body(savedDataNeed);
    }

    @GetMapping
    // Cannot use Page<DataNeed> because then the type information is not serialized; see https://github.com/FasterXML/jackson-databind/issues/2710#issuecomment-624839647
    public ResponseEntity<List<DataNeed>> getAllDataNeeds() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataNeed> getDataNeed(@PathVariable String id) throws DataNeedNotFoundException {
        return service.findById(id)
                      .map(ResponseEntity::ok)
                      .orElseThrow(() -> new DataNeedNotFoundException(id, false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDataNeed(@PathVariable String id) throws DataNeedNotFoundException {
        if (!service.existsById(id)) {
            throw new DataNeedNotFoundException(id, false);
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
