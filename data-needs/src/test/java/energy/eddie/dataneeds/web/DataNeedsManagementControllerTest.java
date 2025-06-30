package energy.eddie.dataneeds.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.dataneeds.DataNeedsSpringConfig;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsDbService;
import energy.eddie.dataneeds.web.management.DataNeedsManagementController;
import energy.eddie.dataneeds.web.management.EnableDisableBody;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.dataneeds.web.DataNeedsControllerTest.EXAMPLE_ACCOUNTING_POINT_DATA_NEED;
import static energy.eddie.dataneeds.web.DataNeedsControllerTest.EXAMPLE_VHD_DATA_NEED;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DataNeedsManagementController.class, properties = {"eddie.management.server.urlprefix=management", "eddie.data-needs-config.data-need-source=database"})
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class DataNeedsManagementControllerTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private DataNeedsDbService mockService;
    private DataNeed exampleVhd;
    private DataNeed exampleAccount;

    public static Stream<Arguments> invalidDataNeedRequests() {
        return Stream.of(
                Arguments.of(
                        "{\"type\":\"validated\",\"policyLink\":\"invalidURL\",\"name\":\"  \",\"description\":\"  \",\"purpose\":\"  \",\"duration\":{\"type\":\"relativeDuration\",\"start\":\"-P1Y\",\"end\":\"P12M\",\"stickyStartCalendarUnit\":null},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT15M\"}",
                        List.of(
                                "name: must not be blank",
                                "description: must not be blank",
                                "purpose: must not be blank",
                                "policyLink: must be a valid URL"
                        )
                ),
                Arguments.of(
                        "{\"type\":\"validated\",\"policyLink\":\"https://example.com/toc\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":null,\"duration\":{\"type\":\"relativeDuration\",\"start\":\"-P1Y\",\"end\":\"P12M\",\"stickyStartCalendarUnit\":null},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT15M\"}",
                        List.of(
                                "purpose: must not be blank"
                        )
                ),
                Arguments.of(
                        "{\"type\":\"validated\",\"policyLink\":\"https://example.com/toc\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":\"purpose\",\"duration\":{\"type\":\"relativeDuration\",\"start\":\"-P1Y\",\"end\":\"P12M\",\"stickyStartCalendarUnit\":null},\"energyType\":\"UNKNOWN_ENUM\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT15M\"}",
                        List.of(
                                "energyType: Invalid enum value: 'UNKNOWN_ENUM'. Valid values: [ELECTRICITY, NATURAL_GAS, HYDROGEN, HEAT]."
                        )
                ),
                Arguments.of(
                        "{\"type\":\"validated\",\"policyLink\":\"https://example.com/toc\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":\"Purpose\",\"duration\":{\"type\":\"relativeDuration\",\"start\":\"P14M\",\"end\":\"P12M\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT15M\"}",
                        List.of(
                                "duration: start must be before or equal to end."
                        )
                ),
                Arguments.of(
                        "{\"type\":\"validated\",\"policyLink\":\"https://example.com/toc\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":\"Purpose\",\"duration\":{\"type\":\"absoluteDuration\",\"start\":\"2024-04-05\",\"end\":\"2024-04-01\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT30M\"}",
                        List.of(
                                "duration: start must be before or equal to end."
                        )
                ),
                Arguments.of(
                        "{\"type\":\"validated\",\"policyLink\":\"https://example.com/toc\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":\"Purpose\",\"duration\":{\"type\":\"absoluteDuration\",\"start\":\"foo\",\"end\":\"2024-04-01\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT30M\"}",
                        List.of(
                                "duration.start: Cannot parse value 'foo'."
                        )
                ),
                Arguments.of(
                        "{\"type\":\"INVALID\",\"policyLink\":\"https://example.com/toc\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":\"Purpose\",\"duration\":{\"type\":\"absoluteDuration\",\"start\":\"2024-04-05\",\"end\":\"2024-04-01\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT30M\"}",
                        List.of(
                                "Invalid request body."
                        )
                ),
                Arguments.of(
                        "{\"type\":\"validated\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":\"purpose\",\"policyLink\":\"https://example.com/toc\",\"duration\":{\"type\":\"relativeDuration\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"P1D\",\"maxGranularity\":\"PT1H\"}",
                        List.of(
                                "dataNeed: maxGranularity must be higher or equal to minGranularity."
                        )
                )
        );
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {
        exampleVhd = mapper.readValue(EXAMPLE_VHD_DATA_NEED, DataNeed.class);
        exampleAccount = mapper.readValue(EXAMPLE_ACCOUNTING_POINT_DATA_NEED, DataNeed.class);
    }

    @ParameterizedTest(name = "{displayName} {1}")
    @DisplayName("Invalid new data need request")
    @MethodSource("invalidDataNeedRequests")
    void givenInvalidDataNeed_createNewDataNeed_returnsBadRequest(
            String json,
            List<String> expectedErrors
    ) throws Exception {
        mockMvc.perform(post("/management")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(expectedErrors.size())))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[*].message",
                                   containsInAnyOrder(expectedErrors.toArray(new String[0]))));
    }

    @Test
    void givenDataNeeds_getAllDataNeeds_returnsAllDataNeeds() throws Exception {
        // Given
        when(mockService.findAll()).thenReturn(List.of(exampleAccount, exampleVhd));

        // When
        mockMvc.perform(get("/management")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", iterableWithSize(2)))
               .andExpect(jsonPath("$[*].id", containsInAnyOrder("123", "fooBar")))
               .andExpect(jsonPath("$[*].name", containsInAnyOrder("Name", "Accounting Point Need")))
               .andExpect(jsonPath("$[*].type", containsInAnyOrder(ValidatedHistoricalDataDataNeed.DISCRIMINATOR_VALUE,
                                                                   AccountingPointDataNeed.DISCRIMINATOR_VALUE)));
    }

    @Test
    void givenNonExistingId_getDataNeed_returnsNotFound() throws Exception {
        // When
        mockMvc.perform(get("/management/{dataNeedId}", "nonExisting")
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNotFound())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   Matchers.is("No data need with ID 'nonExisting' found.")));
    }

    @Test
    void givenExistingId_getDataNeed_returnsDataNeed() throws Exception {
        // Given
        String id = "123";
        DataNeed dataNeed = mapper.readValue(EXAMPLE_VHD_DATA_NEED, DataNeed.class);
        when(mockService.findById(id)).thenReturn(Optional.of(dataNeed));

        // When
        mockMvc.perform(get("/management/{dataNeedId}", id)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id", is(id)))
               .andExpect(jsonPath("$.type", is(ValidatedHistoricalDataDataNeed.DISCRIMINATOR_VALUE)))
               .andExpect(jsonPath("$.duration.start", is("P-90D")))
               .andExpect(jsonPath("$.duration.stickyStartCalendarUnit").doesNotExist());
    }

    @Test
    void givenNonExistingId_deleteDataNeed_returnsNotFound() throws Exception {
        // When
        mockMvc.perform(delete("/management/{dataNeedId}", "nonExisting2")
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNotFound())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   Matchers.is("No data need with ID 'nonExisting2' found.")));
    }

    @Test
    void givenExistingId_deleteDataNeed_callsService_andReturnsNoContent() throws Exception {
        // Given
        String id = "nonExisting2";
        when(mockService.existsById(id)).thenReturn(true);
        doNothing().when(mockService).deleteById(id);

        // When
        mockMvc.perform(delete("/management/{dataNeedId}", id)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNoContent());

        // Then
        verify(mockService).deleteById(id);
    }

    @Test
    void givenExistingId_enableOrDisableDataNeed_callsService_andReturnsNoContent() throws Exception {
        // Given
        String id = "dnid";
        when(mockService.existsById(id)).thenReturn(true);
        doNothing().when(mockService).enableOrDisableDataNeed(id, true);

        // When
        mockMvc.perform(patch("/management/{dataNeedId}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new EnableDisableBody(true))))
               // Then
               .andExpect(status().isNoContent());

        // Then
        verify(mockService).enableOrDisableDataNeed(id, true);
    }

    /**
     * The {@link RegionConnectorsCommonControllerAdvice} is automatically registered for each region connector when the
     * whole core is started. To be able to properly test the controller's error responses, manually add the advice to
     * this test class.
     */
    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
            return new RegionConnectorsCommonControllerAdvice();
        }
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = DataNeedsSpringConfig.class)
@ActiveProfiles("test-data-needs-management-api")
@Sql(scripts = "/test-data-needs-management-api.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DataNeedsManagementFullTest {
    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate restTemplate;
    @MockitoBean
    @SuppressWarnings("unused")
    private SecurityFilterChain mockSecurityFilterChain;

    public static Stream<Arguments> validDataNeedRequests() {
        return Stream.of(
                Arguments.of(
                        "{\"type\":\"validated\",\"name\":\"My awesome data need\",\"description\":\"descr\",\"purpose\":\"purpose\",\"policyLink\":\"https://example.com/toc\",\"duration\":{\"type\":\"relativeDuration\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT1H\",\"maxGranularity\":\"P1D\"}"),
                Arguments.of(
                        "{\"type\":\"outbound-aiida\",\"name\":\"AIIDA Data Need Test\",\"description\":\"With data tags\",\"purpose\":\"Test aiida data need with data tags\",\"policyLink\":\"https://example.com/toc\",\"transmissionSchedule\":\"*/2 * * * * *\",\"asset\":\"CONNECTION-AGREEMENT-POINT\",\"duration\":{\"type\":\"relativeDuration\",\"durationStart\":\"P0D\",\"durationEnd\":\"P10D\"},\"dataTags\":[\"1-0:1.8.0\",\"1-0:1.7.0\"],\"schemas\":[\"SMART-METER-P1-RAW\"]}"),
                Arguments.of(
                        "{\"type\":\"validated\",\"name\":\"NEXT_10_DAYS_ONE_MEASUREMENT_PER_DAY\",\"description\":\"Historical validated consumption data for the next 10 days, one measurement per day\",\"purpose\":\"Some purpose\",\"policyLink\":\"https://example.com/toc\",\"duration\":{\"type\":\"absoluteDuration\",\"start\":\"2024-04-01\",\"end\":\"2024-04-05\"},\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"P1D\",\"maxGranularity\":\"P1D\"}"),
                Arguments.of(
                        "{\"type\":\"account\",\"policyLink\":\"https://example.com/toc\",\"name\":\"My Account Point Data Need\",\"description\":\"Some longtext\",\"purpose\":\"A purpose\"}")
        );
    }

    @ParameterizedTest
    @DisplayName("Valid new data need request")
    @MethodSource("validDataNeedRequests")
    void givenValidDataNeed_returnsCreated(String json) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        ResponseEntity<DataNeed> response = restTemplate.exchange(
                "http://localhost:" + port + "/management",
                HttpMethod.POST,
                entity,
                DataNeed.class);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        System.out.println(mapper.writeValueAsString(response.getBody()));

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertDoesNotThrow(() -> UUID.fromString(response.getBody().id()));
        String expectedLocationHeader = "management/" + response.getBody().id();
        assertNotNull(response.getHeaders().getLocation());
        assertEquals(expectedLocationHeader, response.getHeaders().getLocation().toString());
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfiguration {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/**")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());

            return http.build();
        }
    }
}
