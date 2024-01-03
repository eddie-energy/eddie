package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dataneeds-from-config")
class DataNeedsConfigServiceTest {

    private static final String HISTORICAL_DATA_NEED_KEY = "LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY";
    private static final String REALTIME_DATA_NEED_KEY = "FUTURE_NEAR_REALTIME_DATA";
    public static final String NONEXISTENT_DATA_NEED_ID = "NONEXISTENT_KEY";

    @Autowired
    private DataNeedsConfig dataNeedsConfig;

    @Autowired
    private DataNeedsConfigService dataNeedsConfigService;

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
    void testGetAllDataNeedIds() {
        assertThat(dataNeedsConfigService.getDataNeed(HISTORICAL_DATA_NEED_KEY)).isPresent().get()
                .extracting("type").isEqualTo(DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA);
        assertThat(dataNeedsConfigService.getDataNeed(REALTIME_DATA_NEED_KEY)).isPresent().get()
                .extracting("type").isEqualTo(DataType.AIIDA_NEAR_REALTIME_DATA);
        assertThat(dataNeedsConfigService.getDataNeed(NONEXISTENT_DATA_NEED_ID)).isEmpty();
    }


    @Test
    void testNearRealTimeDataNeedHasTransmissionIntervalAndSharedDataIdsAndServiceName() {
        // When
        Optional<DataNeed> optional = dataNeedsConfigService.getDataNeed("FUTURE_NEAR_REALTIME_DATA");

        // Then
        assertTrue(optional.isPresent());
        assertEquals(10, optional.get().transmissionInterval());
        assertThat(optional.get().sharedDataIds()).hasSameElementsAs(Set.of("1-0:1.8.0", "1-0:1.7.0"));
        assertEquals("Test Service", optional.get().serviceName());
    }
}
