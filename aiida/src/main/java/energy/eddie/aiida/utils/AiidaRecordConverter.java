package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.record.AiidaRecordStreamingDto;
import energy.eddie.aiida.dtos.record.AiidaRecordValueDto;
import energy.eddie.aiida.dtos.record.LatestDataSourceRecordDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;

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

        return new AiidaRecordStreamingDto(aiidaRecord.timestamp(),
                                           aiidaRecord.asset(),
                                           connectionId,
                                           dataNeed.dataNeedId(),
                                           permission.permissionId(),
                                           aiidaRecord.dataSourceId(),
                                           aiidaRecord.aiidaRecordValues());
    }

    /**
     * Converts an {@link AiidaRecord} to a {@link LatestDataSourceRecordDto}.
     *
     * @param aiidaRecord The record to convert.
     * @param dataSource The data source which contains the metadata that should be added to the DTO.
     * @return LatestAiidaRecordDto with its fields populated.
     */
    public static LatestDataSourceRecordDto recordToLatestDto(AiidaRecord aiidaRecord, DataSource dataSource) {
        return new LatestDataSourceRecordDto(aiidaRecord.timestamp(),
                                             dataSource.name(),
                                             aiidaRecord.asset(),
                                             dataSource.id(),
                                             aiidaRecord.aiidaRecordValues()
                                                   .stream()
                                                   .map(AiidaRecordConverter::toValueDto)
                                                   .toList());
    }

    @SuppressWarnings("NullAway")
    private static AiidaRecordValueDto toValueDto(AiidaRecordValue value) {
        return new AiidaRecordValueDto(
                value.rawTag(),
                value.dataTag(),
                value.rawValue(),
                value.rawUnitOfMeasurement(),
                value.value(),
                value.unitOfMeasurement(),
                value.sourceKey()
        );
    }
}
