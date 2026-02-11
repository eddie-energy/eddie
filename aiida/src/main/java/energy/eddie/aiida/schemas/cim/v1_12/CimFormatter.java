package energy.eddie.aiida.schemas.cim.v1_12;

import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.errors.formatter.SchemaFormatterException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.schemas.BaseSchemaFormatter;
import energy.eddie.aiida.schemas.cim.CimFormatterStrategy;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component(value = "cimFormatterVersion112")
public class CimFormatter extends BaseSchemaFormatter {
    private final CimFormatterStrategy<RTDEnvelope> cimFormatterStrategy;

    protected CimFormatter(
            ApplicationInformationService applicationInformationService,
            JsonMapper mapper
    ) {
        super(applicationInformationService, mapper);
        cimFormatterStrategy = new CimStrategy();
    }

    @Override
    public AiidaSchema supportedSchema() {
        return AiidaSchema.SMART_METER_P1_CIM_V1_12;
    }

    @Override
    public byte[] format(
            AiidaRecord aiidaRecord,
            Permission permission
    ) throws SchemaFormatterException {
        try {
            return mapper.writeValueAsBytes(cimFormatterStrategy.toRealTimeDataEnvelope(aiidaId,
                                                                                        aiidaRecord,
                                                                                        permission));
        } catch (JacksonException e) {
            throw new CimSchemaFormatterException(e);
        }
    }
}
