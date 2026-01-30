// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.web;

import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjectionRecord;
import energy.eddie.dataneeds.services.DataNeedsService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DataNeedsController.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
public class DataNeedsControllerTest {
    public static final String EXAMPLE_VHD_DATA_NEED = "{\"type\":\"validated\",\"id\":\"123\",\"name\":\"Name\",\"description\":\"Description\",\"purpose\":\"Purpose\",\"policyLink\":\"https://example.com/toc\",\"createdAt\":1710262490.674,\"energyType\":\"ELECTRICITY\",\"minGranularity\":\"PT15M\",\"maxGranularity\":\"PT15M\",\"duration\":{\"type\":\"relativeDuration\",\"start\":\"-P90D\",\"end\":\"P120D\",\"stickyStartCalendarUnit\":null},\"regionConnectorFilter\":{\"type\":\"blocklist\",\"regionConnectorIds\":[\"foo\",\"bar\"]}}";
    public static final String EXAMPLE_ACCOUNTING_POINT_DATA_NEED = "{\"type\":\"account\",\"id\":\"fooBar\",\"name\":\"Accounting Point Need\",\"description\":\"Description\",\"purpose\":\"Purpose\",\"policyLink\":\"https://example.com/toc\",\"createdAt\":1710262490.674}";
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private DataNeedsService mockDataNeedsService;

    @Test
    void givenNoDataNeeds_getDataNeedIdsAndNames_returnsEmptyResponse() throws Exception {
        // When
        mockMvc.perform(get("/api")
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void givenDataNeeds_getDataNeedIdsAndNames_returnsNameAndIds() throws Exception {
        // Given
        var first = new DataNeedsNameAndIdProjectionRecord("123", "Name");
        var second = new DataNeedsNameAndIdProjectionRecord("fooBar", "Accounting Point Need");
        when(mockDataNeedsService.getDataNeedIdsAndNames()).thenReturn(List.of(first, second));

        // When
        mockMvc.perform(get("/api")
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", iterableWithSize(2)))
               .andExpect(jsonPath("$[*].id", containsInAnyOrder("123", "fooBar")))
               .andExpect(jsonPath("$[*].name", containsInAnyOrder("Name", "Accounting Point Need")));
    }

    @Test
    void givenNonExistingId_getDataNeed_returnsNotFound() throws Exception {
        // Given
        String nonExistingId = "123";

        // When
        mockMvc.perform(get("/api/{dataNeedId}", nonExistingId)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNotFound())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   Matchers.is("No data need with ID '123' found.")));
    }

    @Test
    void givenExistingId_getDataNeed_returnsDataNeed() throws Exception {
        // Given
        String id = "123";
        DataNeed dataNeed = mapper.readValue(EXAMPLE_VHD_DATA_NEED, DataNeed.class);
        when(mockDataNeedsService.findById(id)).thenReturn(Optional.of(dataNeed));

        // When
        mockMvc.perform(get("/api/{dataNeedId}", id)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id", is(id)))
               .andExpect(jsonPath("$.type", is(ValidatedHistoricalDataDataNeed.DISCRIMINATOR_VALUE)))
               .andExpect(jsonPath("$.duration.start", is("P-90D")))
               .andExpect(jsonPath("$.duration.stickyStartCalendarUnit").doesNotExist());
    }

    @Test
    void givenExistingId_getDataNeeds_returnsDataNeeds() throws Exception {
        // Given
        String id = "123";
        DataNeed dataNeed = mapper.readValue(EXAMPLE_VHD_DATA_NEED, DataNeed.class);
        when(mockDataNeedsService.findById(id)).thenReturn(Optional.of(dataNeed));

        // When
        mockMvc.perform(get("/api/")
                                .queryParam("data-need-id", id)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.[0].id", is(id)))
               .andExpect(jsonPath("$.[0].type", is(ValidatedHistoricalDataDataNeed.DISCRIMINATOR_VALUE)))
               .andExpect(jsonPath("$.[0].duration.start", is("P-90D")))
               .andExpect(jsonPath("$.[0].duration.stickyStartCalendarUnit").doesNotExist());
    }

    @Test
    void givenNonExistingId_getDataNeeds_returnsEmptyList() throws Exception {
        // Given
        String id = "123";
        when(mockDataNeedsService.findById(id)).thenReturn(Optional.empty());

        // When
        mockMvc.perform(get("/api/")
                                .queryParam("data-need-id", id)
                                .accept(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$").isEmpty());
    }
}
