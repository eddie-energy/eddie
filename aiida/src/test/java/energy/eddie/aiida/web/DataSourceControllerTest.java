package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.services.DataSourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(DataSourceController.class)
class DataSourceControllerTest {
    private static final UUID dataSourceId = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSourceService service;

    @Test
    @WithMockUser
    void getDataSourceTypes_shouldReturnListOfTypes() throws Exception {
        mockMvc.perform(get("/datasources/types")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(4));
    }


    @Test
    @WithMockUser
    void getAssets_shouldReturnListOfAssets() throws Exception {
        mockMvc.perform(get("/datasources/assets")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @WithMockUser
    void getAllDataSources_shouldReturnListOfDataSources() throws Exception {
        List<DataSource> dataSources = List.of(new DataSource() {});

        when(service.getDataSources()).thenReturn(dataSources);

        mockMvc.perform(get("/datasources")
                                .with(csrf())
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1));

        verify(service, times(1)).getDataSources();
    }

    @Test
    @WithMockUser
    void addDataSource_shouldReturn201() throws Exception {
        doNothing().when(service).addDataSource(any(DataSourceDto.class));

        mockMvc.perform(post("/datasources")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Test Source\",\"dataSourceType\":\"SIMULATION\"}")
                                .with(csrf()))
               .andExpect(status().isCreated());

        verify(service, times(1)).addDataSource(any(DataSourceDto.class));
    }

    @Test
    @WithMockUser
    void addDataSource_shouldReturn400ForInvalidData() throws Exception {
        mockMvc.perform(post("/datasources")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                                .with(csrf()))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void deleteDataSource_shouldReturn200() throws Exception {
        doNothing().when(service).deleteDataSource(dataSourceId);

        mockMvc.perform(delete("/datasources/4211ea05-d4ab-48ff-8613-8f4791a56606")
                                .with(csrf()))
               .andExpect(status().isOk());

        verify(service, times(1)).deleteDataSource(dataSourceId);
    }

    @Test
    @WithMockUser
    void updateDataSource_shouldReturn200() throws Exception {
        mockMvc.perform(patch("/datasources/4211ea05-d4ab-48ff-8613-8f4791a56606")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Source\",\"dataSourceType\":\"SIMULATION\"}")
                                .with(csrf()))
               .andExpect(status().isOk());

        verify(service, times(1)).updateDataSource(any(DataSourceDto.class));
    }

    @Test
    @WithMockUser
    void updateEnabledState_shouldReturn200() throws Exception {
        doNothing().when(service).updateEnabledState(dataSourceId, true);

        mockMvc.perform(patch("/datasources/4211ea05-d4ab-48ff-8613-8f4791a56606/enabled")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("true")
                                .with(csrf()))
               .andExpect(status().isOk());

        verify(service, times(1)).updateEnabledState(dataSourceId, true);
    }

    @Test
    @WithMockUser
    void getDataSourceById_shouldReturnDataSource() throws Exception {
        DataSource mockDataSource = new DataSource() {};
        when(service.getDataSourceById(dataSourceId)).thenReturn(Optional.of(mockDataSource));

        mockMvc.perform(get("/datasources/4211ea05-d4ab-48ff-8613-8f4791a56606")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk());

        verify(service, times(1)).getDataSourceById(dataSourceId);
    }

    @Test
    @WithMockUser
    void getDataSourceById_shouldReturn404IfNotFound() throws Exception {
        when(service.getDataSourceById(dataSourceId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/datasources/4211ea05-d4ab-48ff-8613-8f4791a56606")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound());
    }
}
