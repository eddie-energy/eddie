package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.record.AiidaRecordValueDto;
import energy.eddie.aiida.dtos.record.LatestDataSourceRecordDto;
import energy.eddie.aiida.dtos.record.LatestInboundPermissionRecordDto;
import energy.eddie.aiida.dtos.record.LatestOutboundPermissionRecordDto;
import energy.eddie.aiida.dtos.record.LatestSchemaRecordDto;
import energy.eddie.aiida.errors.*;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.services.LatestRecordService;
import energy.eddie.aiida.utils.ObisCode;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(LatestRecordController.class)
class LatestRecordControllerTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID PERMISSION_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final String TOPIC = "test/topic";
    private static final String SERVER_URI = "mqtt://test.server.com";
    private static final String PAYLOAD = "test-payload-data";
    private static final Instant TIMESTAMP = Instant.parse("2024-01-15T10:30:00Z");
    private static final List<AiidaRecordValueDto> RECORD_VALUES = List.of(
            new AiidaRecordValueDto(
                    ObisCode.POSITIVE_ACTIVE_ENERGY.toString(),
                    ObisCode.POSITIVE_ACTIVE_ENERGY,
                    "25.5",
                    UnitOfMeasurement.WATT,
                    "25.5",
                    UnitOfMeasurement.WATT,
                    ""
            ),
            new AiidaRecordValueDto(
                    ObisCode.NEGATIVE_ACTIVE_ENERGY.toString(),
                    ObisCode.NEGATIVE_ACTIVE_ENERGY,
                    "60.0",
                    UnitOfMeasurement.WATT,
                    "60.0",
                    UnitOfMeasurement.WATT,
                    ""
            )
    );
    private static final LatestDataSourceRecordDto LATEST_RECORD = new LatestDataSourceRecordDto(
            TIMESTAMP,
            "datasource",
            AiidaAsset.SUBMETER,
            DATA_SOURCE_ID,
            RECORD_VALUES
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LatestRecordService service;

    @Test
    @WithMockUser
    void latestAiidaRecord_shouldReturnLatestRecord() throws Exception {
        when(service.latestDataSourceRecord(DATA_SOURCE_ID)).thenReturn(LATEST_RECORD);

        mockMvc.perform(get("/messages/data-source/4211ea05-d4ab-48ff-8613-8f4791a56606/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.timestamp").value("2024-01-15T10:30:00Z"))
               .andExpect(jsonPath("$.asset").value("SUBMETER"))
               .andExpect(jsonPath("$.dataSourceId").value("4211ea05-d4ab-48ff-8613-8f4791a56606"))
               .andExpect(jsonPath("$.values.length()").value(2))
               .andExpect(jsonPath("$.values[0].rawTag").value("1-0:1.8.0"))
               .andExpect(jsonPath("$.values[0].rawValue").value(25.5))
               .andExpect(jsonPath("$.values[1].rawTag").value("1-0:2.8.0"))
               .andExpect(jsonPath("$.values[1].rawValue").value(60.0));

        verify(service, times(1)).latestDataSourceRecord(DATA_SOURCE_ID);
    }

    @Test
    @WithMockUser
    void latestAiidaRecord_shouldReturn404WhenDataSourceNotFound() throws Exception {
        when(service.latestDataSourceRecord(DATA_SOURCE_ID))
                .thenThrow(new LatestAiidaRecordNotFoundException(UUID.randomUUID()));

        mockMvc.perform(get("/messages/data-source/4211ea05-d4ab-48ff-8613-8f4791a56606/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound());

        verify(service, times(1)).latestDataSourceRecord(DATA_SOURCE_ID);
    }

    @Test
    @WithMockUser
    void latestAiidaRecord_shouldReturnRecordWithEmptyValues() throws Exception {
        var recordWithEmptyValues = new LatestDataSourceRecordDto(
                TIMESTAMP,
                "datasource",
                AiidaAsset.SUBMETER,
                DATA_SOURCE_ID,
                List.of()
        );

        when(service.latestDataSourceRecord(DATA_SOURCE_ID)).thenReturn(recordWithEmptyValues);

        mockMvc.perform(get("/messages/data-source/4211ea05-d4ab-48ff-8613-8f4791a56606/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.timestamp").value("2024-01-15T10:30:00Z"))
               .andExpect(jsonPath("$.asset").value("SUBMETER"))
               .andExpect(jsonPath("$.dataSourceId").value("4211ea05-d4ab-48ff-8613-8f4791a56606"))
               .andExpect(jsonPath("$.values.length()").value(0));

        verify(service, times(1)).latestDataSourceRecord(DATA_SOURCE_ID);
    }

    @Test
    @WithMockUser
    void latestAiidaRecord_shouldHandleDifferentAssetTypes() throws Exception {
        var recordWithDifferentAsset = new LatestDataSourceRecordDto(
                TIMESTAMP,
                "datasource",
                AiidaAsset.CONNECTION_AGREEMENT_POINT,
                DATA_SOURCE_ID,
                RECORD_VALUES
        );

        when(service.latestDataSourceRecord(DATA_SOURCE_ID)).thenReturn(recordWithDifferentAsset);

        mockMvc.perform(get("/messages/data-source/4211ea05-d4ab-48ff-8613-8f4791a56606/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.asset").value("CONNECTION-AGREEMENT-POINT"));

        verify(service, times(1)).latestDataSourceRecord(DATA_SOURCE_ID);
    }

    @Test
    @WithMockUser
    void latestAiidaRecord_shouldReturn400ForInvalidUUID() throws Exception {
        mockMvc.perform(get("/messages/data-source/invalid-uuid/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest());

        verify(service, never()).latestDataSourceRecord(any());
    }

    @Test
    @WithMockUser
    void latestOutboundPermissionRecord_shouldReturnLatestRecord() throws Exception {
        var schemaRecords = List.of(
                new LatestSchemaRecordDto(AiidaSchema.SMART_METER_P1_CIM, TIMESTAMP, "message1"),
                new LatestSchemaRecordDto(AiidaSchema.SMART_METER_P1_RAW, TIMESTAMP.plusSeconds(10), "message2")
        );
        var outboundRecord = new LatestOutboundPermissionRecordDto(
                PERMISSION_ID,
                TOPIC,
                SERVER_URI,
                schemaRecords
        );

        when(service.latestOutboundPermissionRecord(PERMISSION_ID)).thenReturn(outboundRecord);

        mockMvc.perform(get("/messages/permission/5211ea05-d4ab-48ff-8613-8f4791a56606/outbound/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.permissionId").value("5211ea05-d4ab-48ff-8613-8f4791a56606"))
               .andExpect(jsonPath("$.topic").value(TOPIC))
               .andExpect(jsonPath("$.serverUri").value(SERVER_URI))
               .andExpect(jsonPath("$.messages.length()").value(2))
               .andExpect(jsonPath("$.messages[0].schema").value("SMART-METER-P1-CIM"))
               .andExpect(jsonPath("$.messages[0].message").value("message1"));

        verify(service, times(1)).latestOutboundPermissionRecord(PERMISSION_ID);
    }

    @Test
    @WithMockUser
    void latestOutboundPermissionRecord_shouldReturn404WhenPermissionNotFound() throws Exception {
        when(service.latestOutboundPermissionRecord(PERMISSION_ID))
                .thenThrow(new LatestPermissionRecordNotFoundException(PERMISSION_ID));

        mockMvc.perform(get("/messages/permission/5211ea05-d4ab-48ff-8613-8f4791a56606/outbound/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound());

        verify(service, times(1)).latestOutboundPermissionRecord(PERMISSION_ID);
    }

    @Test
    @WithMockUser
    void latestOutboundPermissionRecord_shouldReturnRecordWithEmptyMessages() throws Exception {
        var outboundRecord = new LatestOutboundPermissionRecordDto(
                PERMISSION_ID,
                TOPIC,
                SERVER_URI,
                List.of()
        );

        when(service.latestOutboundPermissionRecord(PERMISSION_ID)).thenReturn(outboundRecord);

        mockMvc.perform(get("/messages/permission/5211ea05-d4ab-48ff-8613-8f4791a56606/outbound/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.permissionId").value("5211ea05-d4ab-48ff-8613-8f4791a56606"))
               .andExpect(jsonPath("$.topic").value(TOPIC))
               .andExpect(jsonPath("$.serverUri").value(SERVER_URI))
               .andExpect(jsonPath("$.messages.length()").value(0));

        verify(service, times(1)).latestOutboundPermissionRecord(PERMISSION_ID);
    }

    @Test
    @WithMockUser
    void latestInboundPermissionRecord_shouldReturnLatestRecord() throws Exception {
        var inboundRecord = new LatestInboundPermissionRecordDto(
                TIMESTAMP,
                AiidaAsset.SUBMETER,
                PAYLOAD
        );

        when(service.latestInboundPermissionRecord(PERMISSION_ID)).thenReturn(inboundRecord);

        mockMvc.perform(get("/messages/permission/5211ea05-d4ab-48ff-8613-8f4791a56606/inbound/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.timestamp").value("2024-01-15T10:30:00Z"))
               .andExpect(jsonPath("$.asset").value("SUBMETER"))
               .andExpect(jsonPath("$.payload").value(PAYLOAD));

        verify(service, times(1)).latestInboundPermissionRecord(PERMISSION_ID);
    }

    @Test
    @WithMockUser
    void latestOutboundPermissionRecord_shouldReturn400ForInvalidUUID() throws Exception {
        mockMvc.perform(get("/messages/permission/invalid-uuid/outbound/latest")
                                .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest());

        verify(service, never()).latestOutboundPermissionRecord(any());
    }
}