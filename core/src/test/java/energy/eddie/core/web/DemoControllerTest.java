package energy.eddie.core.web;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;


class DemoControllerTest {

    @WebMvcTest(value = DemoController.class, properties = "eddie.demo.button.enabled=false")
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

    @WebMvcTest(value = DemoController.class, properties = "eddie.demo.button.enabled=true")
    @AutoConfigureMockMvc(addFilters = false)
    @Nested
    class EnabledDemoPageTest {
        @Autowired
        private MockMvc mockMvc;

        @Test
        void testDemoPageEnabled() throws Exception {
            mockMvc.perform(get("/demo"))
                   .andExpect(status().isOk())
                   .andExpect(view().name("demo"));
        }
    }
}