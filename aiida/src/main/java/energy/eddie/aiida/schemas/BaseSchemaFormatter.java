package energy.eddie.aiida.schemas;

import energy.eddie.aiida.services.ApplicationInformationService;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

public abstract class BaseSchemaFormatter implements SchemaFormatter {
    protected final UUID aiidaId;
    protected final JsonMapper mapper;

    protected BaseSchemaFormatter(
            ApplicationInformationService applicationInformationService,
            JsonMapper mapper
    ) {
        this.aiidaId = applicationInformationService.applicationInformation().aiidaId();
        this.mapper = mapper;
    }
}
