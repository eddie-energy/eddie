package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.core.CoreSpringConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.*;

class DataNeedsConfigTest {

    private ConfigurableApplicationContext app;

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.close();
        }
    }

    @Test
    void testDataNeedsFromConfig() {
        app = new SpringApplicationBuilder(CoreSpringConfig.class)
                .properties("eddie.data-needs-config.data-need-source=CONFIG")
                .build()
                .run();
        assertThat(app.getBean(DataNeedsService.class)).isNotNull();
        assertThat(app.getBean(DataNeedsConfigService.class)).isNotNull();
        assertThat(app.getBean(DataNeedsConfig.class)).isNotNull().hasFieldOrProperty("dataNeedForId");
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> app.getBean(DataNeedsDbService.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> app.getBean(DataNeedsDbRepository.class));
    }

    @Test
    void testDataNeedsFromDb() {
        app = new SpringApplicationBuilder(CoreSpringConfig.class)
                .properties("eddie.data-needs-config.data-need-source=DATABASE")
                .build()
                .run();
        assertThat(app.getBean(DataNeedsService.class)).isNotNull();
        assertThat(app.getBean(DataNeedsDbService.class)).isNotNull();
        assertThat(app.getBean(DataNeedsDbRepository.class)).isNotNull();
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> app.getBean(DataNeedsConfigService.class));
    }

    @Test
    void testDataNeedsFromDbWithDataNeedInConfiguration() {
        var unstartedApp = new SpringApplicationBuilder(CoreSpringConfig.class)
                .properties("eddie.data-needs-config.data-need-source=DATABASE",
                        "eddie.data-needs-config.data-needs[0].id=DATA_NEED_ID")
                .build();
        assertThatException().isThrownBy(unstartedApp::run).withRootCauseExactlyInstanceOf(DataNeedsConfig.IllegalConfigurationException.class);
    }
}
