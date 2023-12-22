package energy.eddie.spring.regionconnector.extensions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RegionConnectorConnectorElementProvider.class)
@ContextConfiguration(classes = {RegionConnectorConnectorElementProvider.class, RegionConnectorConnectorElementProviderTest.RegionConnectorConnectorElementProviderTestTestConfig.class})
class RegionConnectorConnectorElementProviderTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenFileOnClasspath_returnsContentsOfFile() throws Exception {
        mockMvc.perform(get("/region-connectors/TestRegionConnector/ce.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("content-type", "application/javascript"))
                .andExpect(content().string(containsString("This is a test javascript file")));
    }

    @TestConfiguration
    protected static class RegionConnectorConnectorElementProviderTestTestConfig {
        @Bean
        @Qualifier(RegionConnectorNameExtension.REGION_CONNECTOR_NAME_BEAN_NAME)
        String regionConnectorName() {
            return "TestRegionConnector";
        }
    }
}
