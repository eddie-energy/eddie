package energy.eddie.framework.web;

import com.google.inject.Inject;
import energy.eddie.framework.MetadataService;
import io.javalin.Javalin;

public class PermissionFacade implements JavalinPathHandler {

    @Inject
    private MetadataService metadataService;

    @Override
    public void registerPathHandlers(Javalin app) {
        app.get("/api/region-connectors-metadata", ctx -> ctx.json(metadataService.getRegionConnectorMetadata()));
    }
}
