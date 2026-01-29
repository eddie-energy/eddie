package energy.eddie.core;

import energy.eddie.core.security.JwtIssuerFilter;
import energy.eddie.core.services.ApplicationInformationService;
import energy.eddie.core.services.DataNeedCalculationRouter;
import energy.eddie.core.services.DataNeedRuleSetRouter;
import energy.eddie.core.services.MetadataService;
import energy.eddie.core.web.PermissionFacadeController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unused")
class CoreSpringConfigTest {
    // Need to use nested classes to be able to pass different properties, didn't work with WebApplicationContextRunner
    @Nested
    @WebMvcTest(
            controllers = PermissionFacadeController.class,
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtIssuerFilter.class)
    )
    @AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
    class NoCorsPropertyTest {
        @Autowired
        private MockMvc mockMvc;
        @MockitoBean
        private MetadataService unusedMetadataService;
        @MockitoBean
        private DataNeedCalculationRouter dataNeedCalculationRouter;
        @MockitoBean
        private DataNeedRuleSetRouter dataNeedRuleSetRouter;
        @MockitoBean
        private ApplicationInformationService unusedApplicationInformationService;

        @Test
        void givenNoCorsMappingProperty_addsNoCorsMapping() throws Exception {
            mockMvc.perform(get("/api/region-connectors-metadata").header("Origin", "https://example.com"))
                   .andExpect(status().isOk())
                   .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Nested
    @WebMvcTest(
            properties = {"eddie.cors.allowed-origins=https://example.com", "eddie.jwt.hmac.secret=mPZzVhT7SJqg9jxuJKdtddswKYt7U1sn49di0eMoFnc=", "eddie.permission.request.timeout.duration=24"},
            controllers = PermissionFacadeController.class,
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtIssuerFilter.class)
    )
    @Import(CoreSecurityConfig.class)
    class GivenCorsPropertyTest {
        @Autowired
        private MockMvc mockMvc;
        @MockitoBean
        private MetadataService unusedMetadataService;
        @MockitoBean
        private DataNeedCalculationRouter dataNeedCalculationRouter;
        @MockitoBean
        private DataNeedRuleSetRouter dataNeedRuleSetRouter;
        @MockitoBean
        private ApplicationInformationService unusedApplicationInformationService;

        @Test
        void givenCorsMappingProperty_addsCorsHeader() throws Exception {
            mockMvc.perform(get("/api/region-connectors-metadata").header("Origin", "https://example.com"))
                   .andExpect(status().isOk())
                   .andExpect(header().string("Access-Control-Allow-Origin", "https://example.com"));
        }

        @Test
        void givenCorsMappingProperty_withWrongOriginHeader_returnsForbidden() throws Exception {
            mockMvc.perform(get("/api/region-connectors-metadata").header("Origin",
                                                                          "https://some-other-not-permitted-domain.com"))
                   .andExpect(status().isForbidden());
        }

        @Test
        void givenCorsMappingProperty_locationHeaderIsExposed() throws Exception {
            mockMvc.perform(get("/api/region-connectors-metadata").header("Origin", "https://example.com"))
                   .andExpect(status().isOk())
                   .andExpect(header().string("Access-Control-Expose-Headers", "Location"));
        }
    }
}
