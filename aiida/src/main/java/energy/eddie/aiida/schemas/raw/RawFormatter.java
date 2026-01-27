package energy.eddie.aiida.schemas.raw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.errors.formatter.RawSchemaFormatterException;
import energy.eddie.aiida.errors.formatter.SchemaFormatterException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.schemas.BaseSchemaFormatter;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.api.agnostic.aiida.AiidaRecordDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.springframework.stereotype.Component;

@Component
public class RawFormatter extends BaseSchemaFormatter {
    public RawFormatter(
            ApplicationInformationService applicationInformationService,
            ObjectMapper mapper
    ) {
        super(applicationInformationService, mapper);
    }

    @Override
    public AiidaSchema supportedSchema() {
        return AiidaSchema.SMART_METER_P1_RAW;
    }

    @Override
    public byte[] format(
            AiidaRecord aiidaRecord,
            Permission permission
    ) throws SchemaFormatterException {
        var aiidaRecordDto = aiidaRecord.toDto(permission.id());
        return serializeOrThrow(aiidaRecordDto);
    }

    private byte[] serializeOrThrow(AiidaRecordDto aiidaRecordDto) throws RawSchemaFormatterException {
        try {
            return mapper.writeValueAsBytes(aiidaRecordDto);
        } catch (JsonProcessingException e) {
            throw new RawSchemaFormatterException(e);
        }
    }
}
