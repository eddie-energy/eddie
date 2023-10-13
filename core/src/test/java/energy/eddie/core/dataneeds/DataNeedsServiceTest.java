package energy.eddie.core.dataneeds;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class DataNeedsServiceTest {

    private static final String HISTORICAL_DATA_NEED_KEY = "LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY";
    private static final String REALTIME_DATA_NEED_KEY = "FUTURE_NEAR_REALTIME_DATA";
    public static final String NONEXISTENT_DATA_NEED_ID = "NONEXISTENT_KEY";

    @Autowired
    private DataNeedsService.DataNeedsConfig dataNeedsConfig;

    @Autowired
    private DataNeedsService dataNeedsService;

    @Test
    void testDataNeedConfigCorrectlyInjected() {
        var dataNeedForId = dataNeedsConfig.getDataNeedForId();
        assertThat(dataNeedForId)
                .hasSize(2)
                .containsKey(HISTORICAL_DATA_NEED_KEY)
                .containsKey(REALTIME_DATA_NEED_KEY)
                .doesNotContainKey(NONEXISTENT_DATA_NEED_ID);
    }

    @Test
    void testGetDataNeed() {
        assertThat(dataNeedsService.getDataNeed(HISTORICAL_DATA_NEED_KEY)).isNotNull()
                .extracting("type").isEqualTo(DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA);
        assertThat(dataNeedsService.getDataNeed(REALTIME_DATA_NEED_KEY)).isNotNull()
                .extracting("type").isEqualTo(DataType.SMART_METER_P1_DATA);
        assertThat(dataNeedsService.getDataNeed(NONEXISTENT_DATA_NEED_ID)).isNull();
    }
}
