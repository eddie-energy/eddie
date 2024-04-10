package energy.eddie.core;

import energy.eddie.core.services.HealthService;
import energy.eddie.core.services.MetadataService;
import energy.eddie.core.web.PermissionFacadeController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoreSpringConfigTest {
    // Need to use nested classes to be able to pass different properties, didn't work with WebApplicationContextRunner
    @Nested
    @WebMvcTest(controllers = PermissionFacadeController.class)
    @AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
    class NoCorsPropertyTest {
        @Autowired
        private MockMvc mockMvc;
        @MockBean
        private MetadataService unusedMetadataService;
        @MockBean
        private HealthService unusedHealthService;

        @Test
        void givenNoCorsMappingProperty_addsNoCorsMapping() throws Exception {
            mockMvc.perform(get("/api/region-connectors-metadata")
                                    .header("Origin", "https://example.com"))
                   .andExpect(status().isOk())
                   .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Nested
    @WebMvcTest(properties = {"eddie.cors.allowed-origins=https://example.com", "eddie.jwt.hmac.secret=mPZzVhT7SJqg9jxuJKdtddswKYt7U1sn49di0eMoFnc="}, controllers = PermissionFacadeController.class)
    @Import(CoreSecurityConfig.class)
    class GivenCorsPropertyTest {
        @Autowired
        private MockMvc mockMvc;
        @MockBean
        private MetadataService unusedMetadataService;
        @MockBean
        private HealthService unusedHealthService;

        @Test
        void givenCorsMappingProperty_addsCorsHeader() throws Exception {
            mockMvc.perform(get("/api/region-connectors-metadata")
                                    .header("Origin", "https://example.com"))
                   .andExpect(status().isOk())
                   .andExpect(header().string("Access-Control-Allow-Origin", "https://example.com"));
        }

        @Test
        void givenCorsMappingProperty_withWrongOriginHeader_returnsForbidden() throws Exception {
            mockMvc.perform(get("/api/region-connectors-metadata")
                                    .header("Origin", "https://some-other-not-permitted-domain.com"))
                   .andExpect(status().isForbidden());
        }

        @Test
        void givenCorsMappingProperty_locationHeaderIsExposed() throws Exception {
            mockMvc.perform(get("/api/region-connectors-metadata")
                                    .header("Origin", "https://example.com"))
                   .andExpect(status().isOk())
                   .andExpect(header().string("Access-Control-Expose-Headers", "Location"));
        }
    }
}
