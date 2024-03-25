package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.AiidaRecordStreamingDto;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.IntegerAiidaRecord;
import energy.eddie.aiida.models.record.StringAiidaRecord;

public class AiidaRecordConverter {
    private AiidaRecordConverter() {
    }

    /**
     * Converts an {@link AiidaRecord} to a {@link AiidaRecordStreamingDto} by adding the required metadata from
     * the permission.
     *
     * @param aiidaRecord The record to convert.
     * @param permission  Permission which contains the metadata that should be added to the DTO.
     * @return AiidaRecordStreamingDto with its fields populated.
     * @throws IllegalArgumentException If the aiidaRecord is an inheritor which has no implementation in this method.
     */
    public static AiidaRecordStreamingDto recordToStreamingDto(AiidaRecord aiidaRecord, Permission permission) {
        Object value;
        if (aiidaRecord instanceof IntegerAiidaRecord intRecord)
            value = intRecord.value();
        else if (aiidaRecord instanceof StringAiidaRecord stringRecord)
            value = stringRecord.value();
        else
            throw new IllegalArgumentException("No conversion logic for this type of record is implemented");

        return new AiidaRecordStreamingDto(aiidaRecord.timestamp(), aiidaRecord.code(), value,
                permission.connectionId(), permission.dataNeedId(), permission.permissionId());
    }
}
