package energy.eddie.core.web;

import energy.eddie.core.security.JwtIssuerFilter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class DemoControllerTest {

    @WebMvcTest(
            value = DemoController.class,
            properties = "eddie.demo.button.enabled=false",
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtIssuerFilter.class)
    )
    @AutoConfigureMockMvc(addFilters = false)
    @Nested
    class DisabledDemoPageTest {
        @Autowired
        private MockMvc mockMvc;

        @Test
        void testDemoPageDisabled() throws Exception {
            mockMvc.perform(get("/demo")).andExpect(status().isNotFound());
        }
    }

    @WebMvcTest(
            value = DemoController.class,
            properties = {"eddie.demo.button.enabled=true", "eddie.public.url=http://localhost:8080"},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtIssuerFilter.class)
    )
    @AutoConfigureMockMvc(addFilters = false)
    @Nested
    class EnabledDemoPageTest {
        @Autowired
        private MockMvc mockMvc;

        @Test
        void testDemoPageEnabled() throws Exception {
            mockMvc.perform(get("/demo"))
                   .andExpect(status().isOk())
                   .andExpect(model().attribute("publicUrl", "http://localhost:8080"))
                   .andExpect(view().name("demo"));
        }
    }
}