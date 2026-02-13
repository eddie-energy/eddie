// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas.cim;

import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.api.agnostic.aiida.ObisCode;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * This is the base class for CIM formatter strategies.
 * Each strategy formats an AIIDA record to a specific CIM version.
 *
 * @param <T> The version-specific RTDEnvelope class.
 * @param <S> The version-specific TimeSeries class.
 * @param <V> The version-specific StandardCodingSchemeTypeList class.
 * @param <W> The version-specific QuantityTypeKind class.
 */
public abstract class BaseCimFormatterStrategy<T, S, V, W> implements CimFormatterStrategy<T> {
    protected static final String DOCUMENT_TYPE = "near-real-time-market-document";
    protected static final String REGION_CONNECTOR = "aiida";
    protected static final ZoneId UTC = ZoneId.of("UTC");
    protected static final String VERSION = "1.0";
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCimFormatterStrategy.class.getName());

    public abstract List<AiidaRecordValue> timeSeriesToAiidaRecordValues(S timeSeries) throws CimSchemaFormatterException;

    protected abstract Map<ObisCode, W> obisToQuantityTypeKindMap();

    protected final BigDecimal toBigDecimalOrThrow(AiidaRecordValue aiidaRecordValue) throws CimSchemaFormatterException {
        try {
            return new BigDecimal(aiidaRecordValue.value());
        } catch (NumberFormatException e) {
            throw new CimSchemaFormatterException(e);
        }
    }

    protected final DataSource dataSourceOfPermissionOrThrow(Permission permission) throws CimSchemaFormatterException {
        var dataSource = permission.dataSource();
        if (dataSource == null) {
            throw new CimSchemaFormatterException(new IllegalArgumentException(
                    "Data source is required for CIM formatting"));
        }

        return dataSource;
    }

    protected final AiidaLocalDataNeed dataNeedOfPermissionOrThrow(Permission permission) throws CimSchemaFormatterException {
        var dataNeed = permission.dataNeed();
        if (dataNeed == null) {
            throw new CimSchemaFormatterException(new IllegalArgumentException(
                    "Data need is required for CIM formatting"));
        }

        return dataNeed;
    }

    protected final boolean isAiidaRecordValueSupported(AiidaRecordValue recordValue) throws CimSchemaFormatterException {
        if (obisToQuantityTypeKindMap().containsKey(getDataTag(recordValue))) {
            return true;
        }

        LOGGER.trace("AIIDA Record Value with data tag {} not supported.", recordValue.dataTag());
        return false;
    }

    @Nullable
    protected final W aiidaRecordValueToQuantityTypeKind(AiidaRecordValue recordValue) throws CimSchemaFormatterException {
        return obisToQuantityTypeKindMap().get(getDataTag(recordValue));
    }

    @Nullable
    protected final V standardCodingSchemeFromValue(String countryCode, Function<String, V> fromValue) {
        try {
            return fromValue.apply("N" + countryCode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.info("Unknown country code.", e);
            return null;
        }
    }

    private ObisCode getDataTag(AiidaRecordValue recordValue) throws CimSchemaFormatterException {
        Objects.requireNonNull(recordValue, "AiidaRecordValue cannot be null");
        var dataTag = recordValue.dataTag();
        if (dataTag == null) {
            throw new CimSchemaFormatterException(new IllegalArgumentException(
                    "Data tag is required for CIM formatting in record: " + recordValue));
        }
        return dataTag;
    }
}
