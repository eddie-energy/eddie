package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SeriesPeriodBuilderFactoryTest {

    @Test
    void createReturnsNewSeriesPeriodBuilder() {
        SeriesPeriodBuilderFactory factory = new SeriesPeriodBuilderFactory();
        SeriesPeriodBuilder builder1 = factory.create();
        SeriesPeriodBuilder builder2 = factory.create();

        assertNotEquals(builder1, builder2);
    }
}
