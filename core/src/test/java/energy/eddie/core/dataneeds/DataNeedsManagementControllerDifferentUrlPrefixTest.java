package energy.eddie.core.dataneeds;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static energy.eddie.core.dataneeds.DataNeedEntityTest.EXAMPLE_DATA_NEED;
import static energy.eddie.core.dataneeds.DataNeedEntityTest.EXAMPLE_DATA_NEED_KEY;
import static energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice.ERRORS_JSON_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DataNeedsManagementController.class, properties = {"eddie.data-needs-config.data-need-source=DATABASE", "management.server.urlprefix=test"})
class DataNeedsManagementControllerDifferentUrlPrefixTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DataNeedsDbRepository repo;

    @Test
    void createDataNeed_alreadyExists_returnsConflict() throws Exception {
        // create new data need via POST
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(true);
        mvc.perform(post("/test/data-needs")
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
    void getDataNeed_noMatch_throwsDataNeedNotFoundException() throws Exception {
        // try to fetch non-existing data need
        given(repo.findById(EXAMPLE_DATA_NEED_KEY)).willReturn(Optional.empty());
        try {
            mvc.perform(get("/test/data-needs/" + EXAMPLE_DATA_NEED_KEY).accept(MediaType.APPLICATION_JSON));
        } catch (ServletException e) {
            assertInstanceOf(DataNeedNotFoundException.class, e.getCause());
        }
    }

    @Test
    void getAllDataNeeds_noDataNeeds_returnsEmptyList() throws Exception {
        var jsonString = mvc.perform(get("/test/data-needs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var result = objectMapper.readValue(jsonString, DataNeedEntity[].class);
        assertThat(result).isEmpty();
    }

    @Test
    void updateDataNeed_idsDontMatch_returnsBadRequest() throws Exception {
        // updates with wrong key should not be processed
        mvc.perform(put("/test/data-needs/" + "wrong-key")
                        .content(objectMapper.writeValueAsString(EXAMPLE_DATA_NEED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
    }

    @Test
    void delete_existingDataNeed_returnsNoContent() throws Exception {
        // all delete requests should be processed
        given(repo.existsById(EXAMPLE_DATA_NEED_KEY)).willReturn(true);
        mvc.perform(delete("/test/data-needs/" + EXAMPLE_DATA_NEED_KEY).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(repo).existsById(EXAMPLE_DATA_NEED_KEY);
        verify(repo).deleteById(EXAMPLE_DATA_NEED_KEY);
        verifyNoMoreInteractions(repo);
    }
}