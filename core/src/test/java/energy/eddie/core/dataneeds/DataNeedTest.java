package energy.eddie.core.dataneeds;

import energy.eddie.api.v0.ConsumptionRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataNeedTest {
    static final String EXAMPLE_DATA_NEED_KEY = "EXAMPLE_DATA_NEED";
    static final DataNeed EXAMPLE_DATA_NEED = new DataNeed(EXAMPLE_DATA_NEED_KEY, "description",
            DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA, ConsumptionRecord.MeteringInterval.PT_15_M, -90, false, 0);

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
}
