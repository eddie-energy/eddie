package energy.eddie.core.dataneeds;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static energy.eddie.core.dataneeds.DataNeedTest.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DataNeedsManagementController.class, properties = "eddie.data-needs-config.data-need-source=DATABASE")
class DataNeedsManagementControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DataNeedsDbRepository repo;

    @Test
    void createDataNeed() throws Exception {
        // create new data need via PUT
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(false);
        mvc.perform(put("/management/data-needs")
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
        verify(repo).existsById(EXAMPLE_DATA_NEED_KEY);
        verify(repo).save(EXAMPLE_DATA_NEED);
        verifyNoMoreInteractions(repo);
        reset(repo);

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
        reset(repo);

        // try to create existing data need
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(true);
        mvc.perform(put("/management/data-needs")
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(repo).existsById(EXAMPLE_DATA_NEED_KEY);
        verifyNoMoreInteractions(repo);
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
        var result = objectMapper.readValue(jsonString, DataNeed[].class);
        assertThat(result).hasSize(3).containsExactly(EXAMPLE_DATA_NEED, dataNeed1, dataNeed2);
        verify(repo).findAll();
        verifyNoMoreInteractions(repo);
        reset(repo);

        // get empty list of data needs
        given(repo.findAll()).willReturn(List.of());
        jsonString = mvc.perform(get("/management/data-needs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        result = objectMapper.readValue(jsonString, DataNeed[].class);
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
        var result = objectMapper.readValue(jsonString, DataNeed.class);
        assertThat(result).isEqualTo(EXAMPLE_DATA_NEED);

        // try to fetch non-existing data need
        given(repo.findById(EXAMPLE_DATA_NEED_KEY)).willReturn(Optional.empty());
        mvc.perform(get("/management/data-needs/" + EXAMPLE_DATA_NEED_KEY).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDataNeed() throws Exception {
        // successfull update
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(true);
        mvc.perform(post("/management/data-needs/" + EXAMPLE_DATA_NEED_KEY)
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().string(""));
        verify(repo).existsById(EXAMPLE_DATA_NEED_KEY);
        verify(repo).save(EXAMPLE_DATA_NEED);
        verifyNoMoreInteractions(repo);
        reset(repo);

        // updates with wrong key should not be processed
        mvc.perform(post("/management/data-needs/" + "wrong-key")
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
    }

    @Test
    void deleteDataNeed() throws Exception {
        // all delete requests should be processed
        mvc.perform(delete("/management/data-needs/" + EXAMPLE_DATA_NEED_KEY).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().string(""));
        verify(repo).deleteById(EXAMPLE_DATA_NEED_KEY);
        verifyNoMoreInteractions(repo);
    }
}