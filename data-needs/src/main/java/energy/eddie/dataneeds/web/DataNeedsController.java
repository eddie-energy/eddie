// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.web;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import energy.eddie.dataneeds.services.DataNeedsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Data needs public API", description = "Used by the frontend to get information about data needs.")
public class DataNeedsController implements DataNeedsControllerOpenApi {

    private final DataNeedsService dataNeedsService;

    public DataNeedsController(DataNeedsService dataNeedsService) {
        this.dataNeedsService = dataNeedsService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<List<DataNeedsNameAndIdProjection>> getDataNeedIdsAndNames() {
        return ResponseEntity.ok(dataNeedsService.getDataNeedIdsAndNames());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<DataNeed> getDataNeeds(@PathVariable String id) throws DataNeedNotFoundException {
        return dataNeedsService.findById(id)
                               .map(ResponseEntity::ok)
                               .orElseThrow(() -> new DataNeedNotFoundException(id, false));
    }

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<List<DataNeed>> getDataNeeds(@RequestParam("data-need-id") Set<String> ids) {
        var dataNeeds = new ArrayList<DataNeed>();
        for (var id : ids) {
            dataNeedsService.findById(id)
                            .ifPresent(dataNeeds::add);
        }
        return ResponseEntity.ok(dataNeeds);
    }
}
