package energy.eddie.core.dataneeds;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dataneeds-from-config")
class DataNeedsConfigServiceTest {

    private static final String HISTORICAL_DATA_NEED_KEY = "LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY";
    private static final String FUTURE_DATA_NEED_KEY = "FUTURE_CONSUMPTION_DATA";
    private static final String INVALID_DATA_NEED_1_KEY = "INVALID_DATA_NEED_1";
    private static final String INVALID_DATA_NEED_2_KEY = "INVALID_DATA_NEED_2";

    public static final String NONEXISTENT_DATA_NEED_ID = "NONEXISTENT_KEY";

    @Autowired
    private DataNeedsConfig dataNeedsConfig;

    @Autowired
    private DataNeedsConfigService dataNeedsConfigService;

    @Test
    void testDataNeedConfigCorrectlyInjected() {
        var dataNeedForId = dataNeedsConfig.getDataNeedForId();
        assertThat(dataNeedForId)
                .containsKey(HISTORICAL_DATA_NEED_KEY)
                .containsKey(FUTURE_DATA_NEED_KEY)
                .containsKey(INVALID_DATA_NEED_1_KEY)
                .containsKey(INVALID_DATA_NEED_2_KEY)
                .doesNotContainKey(NONEXISTENT_DATA_NEED_ID);
    }

    @Test
    void testInvalidDataNeedsAreFiltered() {
        var dataNeedForId = dataNeedsConfig.getDataNeedForId();
        assertThat(dataNeedForId).hasSize(4);
        assertThat(dataNeedsConfigService.getAllDataNeedIds())
                .hasSize(2)
                .containsExactlyInAnyOrder(HISTORICAL_DATA_NEED_KEY, FUTURE_DATA_NEED_KEY);
    }

    @Test
    void testGetAllDataNeedIdsReturnsAllValidOnes() {
        assertThat(dataNeedsConfigService.getDataNeed(HISTORICAL_DATA_NEED_KEY)).isPresent().get()
                .extracting("type").isEqualTo(DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA);
        assertThat(dataNeedsConfigService.getDataNeed(FUTURE_DATA_NEED_KEY)).isPresent().get()
                .extracting("type").isEqualTo(DataType.FUTURE_VALIDATED_CONSUMPTION_DATA);
        assertThat(dataNeedsConfigService.getDataNeed(NONEXISTENT_DATA_NEED_ID)).isEmpty();
    }
}
