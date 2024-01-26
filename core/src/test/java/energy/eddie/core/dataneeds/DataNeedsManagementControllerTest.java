package energy.eddie.core.dataneeds;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.DataType;
import energy.eddie.api.agnostic.Granularity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static energy.eddie.core.dataneeds.DataNeedEntityTest.*;
import static energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice.ERRORS_JSON_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DataNeedsManagementController.class, properties = {"eddie.data-needs-config.data-need-source=DATABASE", "management.server.urlprefix=management"})
class DataNeedsManagementControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DataNeedsDbRepository repo;

    @Test
    void createDataNeed_returnsCreated() throws Exception {
        // create new data need via POST
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(false);
        mvc.perform(post("/management/data-needs")
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
        verify(repo).existsById(EXAMPLE_DATA_NEED_KEY);
        verify(repo).save(EXAMPLE_DATA_NEED);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void createDataNeed_alreadyExists_returnsBadRequest() throws Exception {
        // try to create existing data need
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(true);
        mvc.perform(post("/management/data-needs")
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Data need with ID '" + EXAMPLE_DATA_NEED_KEY + "' already exists.")));
        verify(repo).existsById(EXAMPLE_DATA_NEED_KEY);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void createNewRealTimeDataNeed() throws Exception {
        String dataNeedId = "dn-id";
        var dataNeed = new DataNeedEntity(dataNeedId, "description", DataType.AIIDA_NEAR_REALTIME_DATA,
                Granularity.P1D, -90, false, 0, 10, Set.of("1-0:1.8.0", "1-0:1.7.0"), "MyTestService");
        // create new data need via POST
        given(repo.existsById(dataNeedId)).willReturn(false);
        mvc.perform(post("/management/data-needs")
                        .content(objectMapper.writeValueAsString(dataNeed))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
        verify(repo).existsById(dataNeedId);
        verify(repo).save(dataNeed);
        verifyNoMoreInteractions(repo);
        reset(repo);
    }

    @Test
    void getAllDataNeeds() throws Exception {
        // get 3 data needs
        var dataNeed1 = copy(EXAMPLE_DATA_NEED);
        dataNeed1.setId("1");
        var dataNeed2 = copy(EXAMPLE_DATA_NEED);
        dataNeed2.setId("2");
        dataNeed2.setDurationEnd(-30);
        given(repo.findAll()).willReturn(List.of(EXAMPLE_DATA_NEED, dataNeed1, dataNeed2));
        var jsonString = mvc.perform(get("/management/data-needs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var result = objectMapper.readValue(jsonString, DataNeedEntity[].class);
        assertThat(result).hasSize(3).containsExactly(EXAMPLE_DATA_NEED, dataNeed1, dataNeed2);
        verify(repo).findAll();
        verifyNoMoreInteractions(repo);
        reset(repo);

        // get empty list of data needs
        given(repo.findAll()).willReturn(List.of());
        jsonString = mvc.perform(get("/management/data-needs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        result = objectMapper.readValue(jsonString, DataNeedEntity[].class);
        assertThat(result).isEmpty();
        verify(repo).findAll();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void getDataNeed() throws Exception {
        // get existing data need
        given(repo.findById(EXAMPLE_DATA_NEED_KEY)).willReturn(Optional.of(EXAMPLE_DATA_NEED));
        var jsonString = mvc.perform(get("/management/data-needs/" + EXAMPLE_DATA_NEED_KEY).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var result = objectMapper.readValue(jsonString, DataNeedEntity.class);
        assertThat(result).isEqualTo(EXAMPLE_DATA_NEED);

        // try to fetch non-existing data need
        given(repo.findById(EXAMPLE_DATA_NEED_KEY)).willReturn(Optional.empty());

        // When
        mvc.perform(get("/management/data-needs/" + EXAMPLE_DATA_NEED_KEY).accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("No dataNeed with ID '" + EXAMPLE_DATA_NEED_KEY + "' found.")));
    }

    @Test
    void updateDataNeed_withExistingDataNeed_returnsOk() throws Exception {
        // successful update
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(true);
        mvc.perform(put("/management/data-needs/" + EXAMPLE_DATA_NEED_KEY)
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().string(""));
        verify(repo).existsById(EXAMPLE_DATA_NEED_KEY);
        verify(repo).save(EXAMPLE_DATA_NEED);
        verifyNoMoreInteractions(repo);
        reset(repo);
    }

    @Test
    void updateDataNeed_idsDontMatch_returnsBadRequest() throws Exception {
        // updates with wrong regionConnectorId should not be processed
        mvc.perform(put("/management/data-needs/" + "wrong-key")
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Data need ID in URL does not match data need ID in request body.")));
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
    }

    @Test
    void updateDataNeed_nonExistingId_returnsNotFound() throws Exception {
        // Givem
        when(repo.existsById(EXAMPLE_DATA_NEED_KEY)).thenReturn(false);

        // When
        mvc.perform(put("/management/data-needs/" + EXAMPLE_DATA_NEED_KEY)
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("No dataNeed with ID '%s' found.".formatted(EXAMPLE_DATA_NEED_KEY))));
        verify(repo, never()).save(any());
    }

    @Test
    void delete_existingDataNeed_returnsNoContent() throws Exception {
        // all delete requests should be processed
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(true);
        mvc.perform(delete("/management/data-needs/" + EXAMPLE_DATA_NEED_KEY).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(repo).existsById(EXAMPLE_DATA_NEED_KEY);
        verify(repo).deleteById(EXAMPLE_DATA_NEED_KEY);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void delete_nonExistingDataNeed_returnsNotFound() throws Exception {
        // all delete requests should be processed
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(false);

        // When
        mvc.perform(delete("/management/data-needs/" + EXAMPLE_DATA_NEED_KEY).accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
                .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("No dataNeed with ID '" + EXAMPLE_DATA_NEED_KEY + "' found.")));

        verify(repo).existsById(EXAMPLE_DATA_NEED_KEY);
        verifyNoMoreInteractions(repo);
    }
}