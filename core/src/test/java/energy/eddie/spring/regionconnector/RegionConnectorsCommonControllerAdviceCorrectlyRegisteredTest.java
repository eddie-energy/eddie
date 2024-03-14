package energy.eddie.spring.regionconnector;

import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.core.CoreSpringConfig;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Tests that the {@link RegionConnectorsCommonControllerAdvice} is correctly registered in the region connectors' own
 * context.
 * Also has some example tests to verify that the advice is handling exceptions properly.
 */
@SpringBootTest(classes = CoreSpringConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("common-controller-advice")
@Sql(scripts = "/common-controller-advice.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
class RegionConnectorsCommonControllerAdviceCorrectlyRegisteredTest {
    private static final String PREFIX_SERVLET_ATTRIBUTE_NAME = "org.springframework.web.servlet.FrameworkServlet.CONTEXT.";
    @Autowired
    private WebApplicationContext applicationContext;
    @MockBean
    private DataNeedsService unusedDataNeedsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var esChildContext = getChildContext("es-datadis");
        mockMvc = MockMvcBuilders.webAppContextSetup(esChildContext).build();
    }

    @Test
    void verify_commonControllerAdvice_isRegisteredInEachRegionConnector() {
        var childContext = getChildContext("dk-energinet");
        assertNotNull(childContext.getBean(RegionConnectorsCommonControllerAdvice.class));

        // with the same context, also check that the common controller advice is loaded for a different RC
        childContext = getChildContext("es-datadis");
        assertNotNull(childContext.getBean(RegionConnectorsCommonControllerAdvice.class));
    }

    /**
     * Returns the context of a child that was registered with a {@link ServletRegistrationBean} and the name {@code servletRegistrationName}.
     *
     * @param servletRegistrationName Name of the RC for which to get the context
     * @return WebApplicationContext of the child.
     */
    private WebApplicationContext getChildContext(String servletRegistrationName) {
        assertNotNull(applicationContext.getServletContext());
        var attribute = applicationContext.getServletContext().getAttribute(PREFIX_SERVLET_ATTRIBUTE_NAME + servletRegistrationName);
        assertNotNull(attribute);


        return ((WebApplicationContext) attribute);
    }

    @Test
    void givenInvalidRequestBody_returnsBadRequest() throws Exception {
        // When
        mockMvc.perform(post("/permission-request")
                        // this will cause a HttpMessageNotReadableException
                        .content("")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    void givenIncompleteRequestBody_returnsBadRequest() throws Exception {
        // When
        mockMvc.perform(post("/permission-request")
                        // this will cause a MethodArgumentNotValidException
                        .content("{\"connectionId\":\"\", \"dataNeedId\":\"\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message", hasItems(
                        "nif: must not be null or blank",
                        "meteringPointId: must not be null or blank",
                        "granularity: must not be null",
                        "dataNeedId: must not be null or blank",
                        "connectionId: must not be null or blank",
                        "requestDataTo: must not be null",
                        "requestDataFrom: must not be null")));
    }

    @Test
    void givenNonExistingPermissionId_handlePermissionNotFoundException_returnsNotFound() throws Exception {
        // When
        mockMvc.perform(get("/permission-status/NonExistingId"))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("No permission with ID 'NonExistingId' found.")));
    }
}
