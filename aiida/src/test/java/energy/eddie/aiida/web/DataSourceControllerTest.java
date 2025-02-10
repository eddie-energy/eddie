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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(DataSourceController.class)
class DataSourceControllerTest {

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
        doNothing().when(service).deleteDataSource(1L);

        mockMvc.perform(delete("/datasources/1")
                                .with(csrf()))
               .andExpect(status().isOk());

        verify(service, times(1)).deleteDataSource(1L);
    }

    @Test
    @WithMockUser
    void updateDataSource_shouldReturn200() throws Exception {
        mockMvc.perform(patch("/datasources/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Source\",\"dataSourceType\":\"SIMULATION\"}")
                                .with(csrf()))
               .andExpect(status().isOk());

        verify(service, times(1)).updateDataSource(any(DataSourceDto.class));
    }

    @Test
    @WithMockUser
    void updateEnabledState_shouldReturn200() throws Exception {
        doNothing().when(service).updateEnabledState(1L, true);

        mockMvc.perform(patch("/datasources/1/enabled")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("true")
                                .with(csrf()))
               .andExpect(status().isOk());

        verify(service, times(1)).updateEnabledState(1L, true);
    }

    @Test
    @WithMockUser
    void getDataSourceById_shouldReturnDataSource() throws Exception {
        DataSource mockDataSource = new DataSource() {};
        when(service.getDataSourceById(1L)).thenReturn(Optional.of(mockDataSource));

        mockMvc.perform(get("/datasources/1")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk());

        verify(service, times(1)).getDataSourceById(1L);
    }

    @Test
    @WithMockUser
    void getDataSourceById_shouldReturn404IfNotFound() throws Exception {
        when(service.getDataSourceById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/datasources/1")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound());
    }
}
