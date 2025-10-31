package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.services.ApplicationInformationService;

import java.util.UUID;

public abstract class BaseSchemaFormatter implements SchemaFormatter {
    protected final UUID aiidaId;
    protected final ObjectMapper mapper;

    protected BaseSchemaFormatter(
            ApplicationInformationService applicationInformationService,
            ObjectMapper mapper
    ) {
        this.aiidaId = applicationInformationService.applicationInformation().aiidaId();
        this.mapper = mapper;
    }
}
