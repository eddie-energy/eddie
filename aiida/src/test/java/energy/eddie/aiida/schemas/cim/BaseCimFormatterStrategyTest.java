// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas.cim;

import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.api.agnostic.aiida.ObisCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static energy.eddie.api.agnostic.aiida.UnitOfMeasurement.KILO_WATT_HOUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseCimFormatterStrategyTest {
    private final TestStrategy strategy = new TestStrategy();

    @Test
    void toBigDecimalOrThrow_returnsBigDecimalForValidInput() throws CimSchemaFormatterException {
        // Given
        var recordValue = recordValue(POSITIVE_ACTIVE_ENERGY, "123.45");

        // When
        var result = strategy.toBigDecimalOrThrow(recordValue);

        // Then
        assertThat(result).isEqualByComparingTo(new BigDecimal("123.45"));
    }

    @Test
    void toBigDecimalOrThrow_wrapsNumberFormatException() {
        // Given
        var recordValue = recordValue(POSITIVE_ACTIVE_ENERGY, "not-a-number");

        // When / Then
        assertThatThrownBy(() -> strategy.toBigDecimalOrThrow(recordValue))
                .isInstanceOf(CimSchemaFormatterException.class)
                .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void dataSourceOfPermissionOrThrow_returnsDataSourceWhenPresent() throws CimSchemaFormatterException {
        // Given
        var permission = mock(Permission.class);
        var dataSource = mock(DataSource.class);
        when(permission.dataSource()).thenReturn(dataSource);

        // When
        var result = strategy.dataSourceOfPermissionOrThrow(permission);

        // Then
        assertThat(result).isSameAs(dataSource);
    }

    @Test
    void dataSourceOfPermissionOrThrow_throwsWhenMissing() {
        // Given
        var permission = mock(Permission.class);
        when(permission.dataSource()).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> strategy.dataSourceOfPermissionOrThrow(permission))
                .isInstanceOf(CimSchemaFormatterException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Data source is required for CIM formatting");
    }

    @Test
    void dataNeedOfPermissionOrThrow_returnsDataNeedWhenPresent() throws CimSchemaFormatterException {
        // Given
        var permission = mock(Permission.class);
        var dataNeed = mock(AiidaLocalDataNeed.class);
        when(permission.dataNeed()).thenReturn(dataNeed);

        // When
        var result = strategy.dataNeedOfPermissionOrThrow(permission);

        // Then
        assertThat(result).isSameAs(dataNeed);
    }

    @Test
    void dataNeedOfPermissionOrThrow_throwsWhenMissing() {
        // Given
        var permission = mock(Permission.class);
        when(permission.dataNeed()).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> strategy.dataNeedOfPermissionOrThrow(permission))
                .isInstanceOf(CimSchemaFormatterException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Data need is required for CIM formatting");
    }

    @Test
    void isAiidaRecordValueSupported_returnsTrueForMappedObisCode() {
        // Given
        var recordValue = recordValue(POSITIVE_ACTIVE_ENERGY, "1");

        // When / Then
        assertThat(strategy.isAiidaRecordValueSupported(recordValue)).isTrue();
    }

    @Test
    void isAiidaRecordValueSupported_returnsFalseForUnmappedObisCode() {
        // Given
        var recordValue = recordValue(ObisCode.DEVICE_ID_1, "device-id");

        // When / Then
        assertThat(strategy.isAiidaRecordValueSupported(recordValue)).isFalse();
    }

    @Test
    void isAiidaRecordValueSupported_returnsFalseWhenDataTagMissing() {
        // Given
        var recordValue = recordValue(null, "42");

        // When / Then
        assertThat(strategy.isAiidaRecordValueSupported(recordValue)).isFalse();
    }

    @Test
    void aiidaRecordValueToQuantityTypeKind_returnsMappedValue() throws CimSchemaFormatterException {
        // Given
        var recordValue = recordValue(POSITIVE_ACTIVE_ENERGY, "1");

        // When / Then
        assertThat(strategy.aiidaRecordValueToQuantityTypeKind(recordValue)).isEqualTo("SUPPORTED");
    }

    @Test
    void aiidaRecordValueToQuantityTypeKind_returnsNullForUnmappedValue() throws CimSchemaFormatterException {
        // Given
        var recordValue = recordValue(ObisCode.DEVICE_ID_1, "1");

        // When / Then
        assertThat(strategy.aiidaRecordValueToQuantityTypeKind(recordValue)).isNull();
    }

    @Test
    void standardCodingSchemeFromValue_returnsTransformedCountryCode() {
        // Given, When
        var result = strategy.standardCodingSchemeFromValue("at", value -> "mapped-" + value);

        // Then
        assertThat(result).isEqualTo("mapped-NAT");
    }

    @Test
    void standardCodingSchemeFromValue_returnsNullOnIllegalArgumentException() {
        // Given, When
        var result = strategy.standardCodingSchemeFromValue("at", value -> {
            throw new IllegalArgumentException("unknown");
        });

        // Then
        assertThat(result).isNull();
    }

    private static AiidaRecordValue recordValue(ObisCode dataTag, String value) {
        return new AiidaRecordValue(
                "raw-tag",
                dataTag,
                value,
                KILO_WATT_HOUR,
                value,
                KILO_WATT_HOUR
        );
    }

    private static class TestStrategy extends BaseCimFormatterStrategy<Object, Object, String, String> {
        @Override
        public Object toRealTimeDataEnvelope(UUID aiidaId, AiidaRecord aiidaRecord, Permission permission) {
            return new Object();
        }

        @Override
        public List<AiidaRecordValue> timeSeriesToAiidaRecordValues(Object timeSeries) {
            return List.of();
        }

        @Override
        protected Map<ObisCode, String> obisToQuantityTypeKindMap() {
            return Map.of(POSITIVE_ACTIVE_ENERGY, "SUPPORTED");
        }
    }
}
