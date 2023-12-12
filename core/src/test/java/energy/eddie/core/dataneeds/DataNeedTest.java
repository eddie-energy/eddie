package energy.eddie.core.dataneeds;

import energy.eddie.api.v0.ConsumptionRecord;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

class DataNeedTest {
    static final String EXAMPLE_DATA_NEED_KEY = "EXAMPLE_DATA_NEED";
    static final DataNeed EXAMPLE_DATA_NEED = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
            DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA, ConsumptionRecord.MeteringInterval.PT_15_M, -90, false, 0);

    private final Validator validator;

    DataNeedTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    static DataNeed copy(DataNeed dataNeed) {
        return new DataNeed(dataNeed.getId(), dataNeed.getDescription(), dataNeed.getType(), dataNeed.getGranularity(),
                dataNeed.getDurationStart(), dataNeed.getDurationOpenEnd(), dataNeed.getDurationEnd());
    }

    @Test
    void testEqualsAndHashcode() {
        var equalDataNeed = copy(EXAMPLE_DATA_NEED);
        assertThat(EXAMPLE_DATA_NEED).isEqualTo(equalDataNeed).hasSameHashCodeAs(equalDataNeed).isNotSameAs(equalDataNeed);
        var differentKeyDataNeed = copy(EXAMPLE_DATA_NEED);
        differentKeyDataNeed.setId(EXAMPLE_DATA_NEED.getId() + "_changed");
        assertThat(EXAMPLE_DATA_NEED).isNotEqualTo(differentKeyDataNeed).doesNotHaveSameHashCodeAs(differentKeyDataNeed);
        var differentTypeDataNeed = copy(EXAMPLE_DATA_NEED);
        differentTypeDataNeed.setType(DataType.FUTURE_VALIDATED_CONSUMPTION_DATA);
        assertThat(EXAMPLE_DATA_NEED).isNotEqualTo(differentTypeDataNeed).doesNotHaveSameHashCodeAs(differentTypeDataNeed);
    }

    @Test
    @SuppressWarnings("NullAway")
    void testMandatoryFieldsArePresent() {
        var mandatoryFields = new String[]{"id", "description", "type", "durationStart"};
        var dataNeed = new DataNeed("", "", null, null, null, null, null);
        var errors = validator.validate(dataNeed);
        var violatedProperties = errors.stream().map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
        assertThat(violatedProperties).containsExactlyInAnyOrder(mandatoryFields);

        dataNeed = new DataNeed(null, null, null, null, null, null, null);
        errors = validator.validate(dataNeed);
        violatedProperties = errors.stream().map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
        assertThat(violatedProperties).containsExactlyInAnyOrder(mandatoryFields);
    }


    @Test
    void testDurationStartPreceedsDurationEnd() {
        var violations = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
                DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA, ConsumptionRecord.MeteringInterval.PT_15_M, 0, false, -1).validate(validator);
        assertThat(violations).hasSize(1).first(as(STRING)).contains("durationEnd").contains("durationStart");
    }

    @Test
    void testHistoricalValidatedConsumptionDataIsForThePastOnly() {
        var violations = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
                DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA, ConsumptionRecord.MeteringInterval.PT_15_M, -90, false, 1).validate(validator);
        assertThat(violations).hasSize(1).first(as(STRING)).contains("durationEnd");

        violations = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
                DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA, ConsumptionRecord.MeteringInterval.PT_15_M, -90, true, 0).validate(validator);
        assertThat(violations).filteredOn(m -> m.contains("durationEnd")).hasSize(1);
    }

    @Test
    void testFutureValidatedConsumptionDataIsForTheFutureOnly() {
        var violations = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
                DataType.FUTURE_VALIDATED_CONSUMPTION_DATA, ConsumptionRecord.MeteringInterval.PT_15_M, -1, true, null).validate(validator);
        assertThat(violations).hasSize(1).first(as(STRING)).contains("durationStart");
    }

    @Test
    void testDurationEndMandatoryExceptForAccountingPointMasterData() {
        for (var type : DataType.values()) {
            var violations = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
                    type, ConsumptionRecord.MeteringInterval.PT_15_M, -1, null, null).validate(validator);
            if (type == DataType.ACCOUNTING_POINT_MASTER_DATA) {
                assertThat(violations).isEmpty();
            } else {
                assertThat(violations).filteredOn(m -> m.contains("durationEnd")).isNotEmpty();
            }
        }
    }

    @Test
    void testDurationOpenEndOnlyAllowedForFutureData() {
        for (var type : DataType.values()) {
            var violations = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
                    type, ConsumptionRecord.MeteringInterval.PT_15_M, 0, true, null).validate(validator);
            if (type == DataType.FUTURE_VALIDATED_CONSUMPTION_DATA || type == DataType.SMART_METER_P1_DATA) {
                assertThat(violations).isEmpty();
            } else {
                assertThat(violations).filteredOn(m -> m.contains("durationOpenEnd")).isNotEmpty();
            }
        }
    }

    @Test
    void testP1DataMayStartInThePastAndEndInFuture() {
        var violations = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
                DataType.SMART_METER_P1_DATA, ConsumptionRecord.MeteringInterval.PT_5_M, -100, false, 100).validate(validator);
        assertThat(violations).isEmpty();
    }

    @Test
    void testP1DataMayBeForFutureOnly() {
        var violations = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
                DataType.SMART_METER_P1_DATA, ConsumptionRecord.MeteringInterval.PT_5_M, 0, false, 100).validate(validator);
        assertThat(violations).isEmpty();
    }
}
