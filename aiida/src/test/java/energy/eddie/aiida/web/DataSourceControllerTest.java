package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.DataSourceSecretsDto;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.simulation.SimulationDataSource;
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
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(DataSourceController.class)
class DataSourceControllerTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final SimulationDataSource DATA_SOURCE = mock(SimulationDataSource.class);
    private static final String PLAIN_TEXT_PASSWORD = "SUPER_SAFE";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSourceService service;

    @Test
    @WithMockUser
    void getAssets_shouldReturnListOfAssets() throws Exception {
        mockMvc.perform(get("/datasources/assets")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("assets.length()").value(4));
    }

    @Test
    @WithMockUser
    void getAllOutboundDataSources_shouldReturnListOfDataSources() throws Exception {
        List<DataSource> dataSources = List.of(DATA_SOURCE);

        when(service.getOutboundDataSources()).thenReturn(dataSources);

        mockMvc.perform(get("/datasources/outbound")
                                .with(csrf())
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1));

        verify(service, times(1)).getOutboundDataSources();
    }

    @Test
    @WithMockUser
    void getAllInboundDataSources_shouldReturnListOfDataSources() throws Exception {
        List<DataSource> dataSources = List.of(DATA_SOURCE);

        when(service.getInboundDataSources()).thenReturn(dataSources);

        mockMvc.perform(get("/datasources/inbound")
                                .with(csrf())
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1));

        verify(service, times(1)).getInboundDataSources();
    }

    @Test
    @WithMockUser
    void addDataSource_shouldReturn201() throws Exception {
        doReturn(new DataSourceSecretsDto(DATA_SOURCE_ID, PLAIN_TEXT_PASSWORD))
                .when(service).addDataSource(any(DataSourceDto.class));

        mockMvc.perform(post("/datasources")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Test Source\",\"dataSourceType\":\"SIMULATION\"}")
                                .with(csrf()))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.plaintextPassword").value(PLAIN_TEXT_PASSWORD));

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
        doNothing().when(service).deleteDataSource(DATA_SOURCE_ID);

        mockMvc.perform(delete("/datasources/4211ea05-d4ab-48ff-8613-8f4791a56606")
                                .with(csrf()))
               .andExpect(status().isOk());

        verify(service, times(1)).deleteDataSource(DATA_SOURCE_ID);
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
        doNothing().when(service).updateEnabledState(DATA_SOURCE_ID, true);

        mockMvc.perform(patch("/datasources/4211ea05-d4ab-48ff-8613-8f4791a56606/enabled")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("true")
                                .with(csrf()))
               .andExpect(status().isOk());

        verify(service, times(1)).updateEnabledState(DATA_SOURCE_ID, true);
    }

    @Test
    @WithMockUser
    void getdataSourceByIdOrThrow_shouldReturnDataSource() throws Exception {
        when(service.dataSourceByIdOrThrow(DATA_SOURCE_ID)).thenReturn(DATA_SOURCE);

        mockMvc.perform(get("/datasources/4211ea05-d4ab-48ff-8613-8f4791a56606")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk());

        verify(service, times(1)).dataSourceByIdOrThrow(DATA_SOURCE_ID);
    }

    @Test
    @WithMockUser
    void getdataSourceByIdOrThrow_shouldReturn404IfNotFound() throws Exception {
        when(service.dataSourceByIdOrThrow(DATA_SOURCE_ID)).thenThrow(DataSourceNotFoundException.class);

        mockMvc.perform(get("/datasources/4211ea05-d4ab-48ff-8613-8f4791a56606")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void regenerateSecrets_shouldReturn200() throws Exception {
        doReturn(new DataSourceSecretsDto(DATA_SOURCE_ID, PLAIN_TEXT_PASSWORD))
                .when(service).regenerateSecrets(DATA_SOURCE_ID);

        mockMvc.perform(post("/datasources/%s/regenerate-secrets".formatted(DATA_SOURCE_ID))
                                .accept(MediaType.APPLICATION_JSON)
                                .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.plaintextPassword").value(PLAIN_TEXT_PASSWORD));

        verify(service, times(1)).regenerateSecrets(DATA_SOURCE_ID);
    }
}
