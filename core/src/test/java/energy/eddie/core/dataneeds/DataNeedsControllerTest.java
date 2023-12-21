package energy.eddie.core.dataneeds;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.DataType;
import energy.eddie.api.v0.ConsumptionRecord;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataNeedsController.class)
class DataNeedsControllerTest {

    /**
     * Despite the general rule that constructor injection is preferred over field injection, it's used here anyway.
     * MockBeans cannot be injected via constructor injection, so we use field injection for all dependencies in this test.
     */
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DataNeedsConfigService dataNeedsConfigService;

    @Test
    void testGetDataNeed() throws Exception {
        final var dataNeed = new DataNeedEntity("dn-id", "description", DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA,
                ConsumptionRecord.MeteringInterval.P_1_D, -90, false, 0);
        given(this.dataNeedsConfigService.getDataNeed("dn-id"))
                .willReturn(Optional.of(dataNeed));
        mvc.perform(get("/api/data-needs/dn-id").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(dataNeed), true));
        mvc.perform(get("/api/data-needs/nonexistent-id").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDataTypes() throws Exception {
        var dataTypesJson = mvc.perform(get("/api/data-needs/types").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        // assert that we have a valid JSON array
        assertThat(new JSONArray(dataTypesJson)).isNotNull();
        // assert that we have all possible values in there
        assertThat(objectMapper.readValue(dataTypesJson, new TypeReference<Set<DataType>>() {
        }))
                .isNotNull()
                .hasSize(DataType.values().length);
    }

    @Test
    void testGetDataGranularities() throws Exception {
        var dataGranularitiesJson = mvc.perform(get("/api/data-needs/granularities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        // assert that we have a valid JSON array
        assertThat(new JSONArray(dataGranularitiesJson)).isNotNull();
        // assert that we have all possible values in there
        assertThat(objectMapper.readValue(dataGranularitiesJson, new TypeReference<Set<ConsumptionRecord.MeteringInterval>>() {
        }))
                .isNotNull()
                .hasSize(ConsumptionRecord.MeteringInterval.values().length);
    }
}
