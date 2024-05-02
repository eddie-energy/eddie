package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TimeSeriesBuilderFactoryTest {

    @Test
    void createReturnsNewTimeSeriesBuilder() {
        TimeSeriesBuilderFactory factory = new TimeSeriesBuilderFactory();
        TimeSeriesBuilder builder1 = factory.create();
        TimeSeriesBuilder builder2 = factory.create();

        assertNotEquals(builder1, builder2);
    }
}
