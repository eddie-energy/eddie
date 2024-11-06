package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.AiidaRecordStreamingDto;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;

import static java.util.Objects.requireNonNull;

public class AiidaRecordConverter {
    private AiidaRecordConverter() {
    }

    /**
     * Converts an {@link AiidaRecord} to a {@link AiidaRecordStreamingDto} by adding the required metadata from the
     * permission.
     *
     * @param aiidaRecord The record to convert.
     * @param permission  Permission which contains the metadata that should be added to the DTO.
     * @return AiidaRecordStreamingDto with its fields populated.
     * @throws IllegalArgumentException If the aiidaRecord is an inheritor which has no implementation in this method.
     */
    public static AiidaRecordStreamingDto recordToStreamingDto(AiidaRecord aiidaRecord, Permission permission) {
        var connectionId = requireNonNull(permission.connectionId());
        var dataNeed = requireNonNull(permission.dataNeed());

        return new AiidaRecordStreamingDto(aiidaRecord.timestamp(), aiidaRecord.asset(),
                                           connectionId, dataNeed.dataNeedId(), permission.permissionId(), aiidaRecord.aiidaRecordValue());
    }
}
