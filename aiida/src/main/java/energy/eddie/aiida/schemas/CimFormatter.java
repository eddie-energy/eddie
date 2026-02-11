// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas;

import energy.eddie.aiida.errors.formatter.CimFormatterException;
import energy.eddie.aiida.errors.formatter.FormatterException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.schemas.cim.v1_04.utils.CimUtil;
import energy.eddie.aiida.utils.CimUtils;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.cim.v1_04.StandardQualityTypeList;
import energy.eddie.cim.v1_04.rtd.*;
import jakarta.annotation.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

public class CimFormatter extends SchemaFormatter {
    private static final String DOCUMENT_TYPE = "near-real-time-market-document";
    private static final String REGION_CONNECTOR = "aiida";
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final String VERSION = "1.0";

    private final UUID aiidaId;

    public CimFormatter(UUID aiidaId) {
        this.aiidaId = aiidaId;
    }

    @Override
    public byte[] toSchema(
            AiidaRecord aiidaRecord,
            ObjectMapper mapper,
            Permission permission
    ) throws FormatterException {
        try {
            return mapper.writeValueAsBytes(toEnvelope(aiidaRecord, permission));
        } catch (JacksonException e) {
            throw new CimFormatterException(e);
        }
    }

    private RTDEnvelope toEnvelope(AiidaRecord aiidaRecord, Permission permission) throws CimFormatterException {
        var dataNeed = CimUtil.dataNeedOfPermissionOrThrow(permission);
        var countryCode = CimUtil.dataSourceOfPermissionOrThrow(permission).countryCode();
        var codingScheme = CimUtils.codingSchemeFromCountryCode(countryCode);
        var codingSchemeValue = codingScheme != null ? codingScheme.value() : null;

        return new RTDEnvelope().withMarketDocument(toMarketDocument(aiidaRecord, codingSchemeValue))
                                .withMessageDocumentHeaderCreationDateTime(ZonedDateTime.now(UTC))
                                .withMessageDocumentHeaderMetaInformationAsset(aiidaRecord.asset().toString())
                                .withMessageDocumentHeaderMetaInformationConnectionId(permission.connectionId())
                                .withMessageDocumentHeaderMetaInformationDataNeedId(dataNeed.dataNeedId().toString())
                                .withMessageDocumentHeaderMetaInformationDataSourceId(aiidaRecord.dataSourceId()
                                                                                                 .toString())
                                .withMessageDocumentHeaderMetaInformationDocumentType(DOCUMENT_TYPE)
                                .withMessageDocumentHeaderMetaInformationFinalCustomerId(aiidaId.toString())
                                .withMessageDocumentHeaderMetaInformationPermissionId(permission.id().toString())
                                .withMessageDocumentHeaderMetaInformationRegionConnector(REGION_CONNECTOR)
                                .withMessageDocumentHeaderMetaInformationRegionCountry(countryCode);
    }

    private RTDMarketDocument toMarketDocument(
            AiidaRecord aiidaRecord,
            @Nullable String codingScheme
    ) throws CimFormatterException {
        return new RTDMarketDocument()
                .withCreatedDateTime(ZonedDateTime.now(UTC))
                .withMRID(UUID.randomUUID().toString())
                .withTimeSeries(toTimeSeries(aiidaRecord, codingScheme));
    }

    private TimeSeries toTimeSeries(
            AiidaRecord aiidaRecord,
            @Nullable String codingScheme
    ) throws CimFormatterException {

        var deviceId = aiidaRecord.aiidaRecordValues()
                .stream()
                .filter(value -> value
                        .dataTag()
                        .equals(ObisCode.DEVICE_ID_1))
                .findFirst();
        String mRID = deviceId.isPresent() ? deviceId.get().value() : aiidaRecord.dataSourceId().toString();

        return new TimeSeries().withDateAndOrTimeDateTime(aiidaRecord.timestamp().atZone(UTC))
                               .withQuantities(aiidaRecord
                                                       .aiidaRecordValues()
                                                       .stream()
                                                       .filter(CimUtil::isAiidaRecordValueSupported)
                                                       .map(aiidaRecordValue -> new Quantity()
                                                               .withQuality(StandardQualityTypeList.AS_PROVIDED.toString())
                                                               .withQuantity(toBigDecimal(aiidaRecordValue))
                                                               .withType(CimUtil.aiidaRecordValueToQuantityTypeKind(
                                                                       aiidaRecordValue)))
                                                       .toList())
                               .withRegisteredResourceMRID(new ResourceIDString()
                                                       .withCodingScheme(codingScheme)
                                                       .withValue(mRID))
                               .withVersion(VERSION);
    }

    private BigDecimal toBigDecimal(AiidaRecordValue aiidaRecordValue) throws CimFormatterException {
        try {
            return new BigDecimal(aiidaRecordValue.value());
        } catch (NumberFormatException e) {
            throw new CimFormatterException(e);
        }
    }
}
