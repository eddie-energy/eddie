package energy.eddie.regionconnector.at.eda.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationService;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class})
@Import(PermissionRequestController.class)
class PermissionRequestControllerTest {

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PermissionRequestService permissionRequestService;
    @MockBean
    private PermissionRequestCreationService permissionRequestCreationService;
    @MockBean
    private ServletWebServerApplicationContext unusedServerApplicationContext;
    @MockBean
    private Supplier<Integer> unusedPortSupplier;

    private static Stream<Arguments> permissionRequestArguments() {
        return Stream.of(
                Arguments.of("", "0".repeat(33), "dnid", "0".repeat(8)),
                Arguments.of("cid", "", "dnid", "0".repeat(8)),
                Arguments.of("cid", "0".repeat(33), "", "0".repeat(8)),
                Arguments.of("cid", "0".repeat(33), "dnid", ""),
                Arguments.of(null, "0".repeat(33), "dnid", "0".repeat(8)),
                Arguments.of("cid", "0".repeat(33), null, "0".repeat(8))
        );
    }

    @Test
    void javascriptConnectorElement_returnsOk() throws Exception {
        // Given

        // When
        mockMvc.perform(MockMvcRequestBuilders.get("/region-connectors/at-eda/ce.js"))
                // Then
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    void permissionStatus_permissionExists_returnsOk() throws Exception {
        // Given
        var state = new AtAcceptedPermissionRequestState(null);
        String permissionId = "permissionId";
        when(permissionRequestService.findConnectionStatusMessageById(permissionId))
                .thenReturn(Optional.of(new ConnectionStatusMessage(
                        "cid",
                        permissionId,
                        "dnid",
                        new EdaDataSourceInformation("dsoId"),
                        state.status(),
                        ""
                )));
        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/region-connectors/at-eda/permission-status/{permissionId}", permissionId)
                                .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionId", is(permissionId)))
                .andExpect(jsonPath("$.connectionId", is("cid")));
    }

    @Test
    void permissionStatus_permissionDoesNotExist_returnsNotFound() throws Exception {
        // Given
        var state = new AtAcceptedPermissionRequestState(null);
        when(permissionRequestService.findByPermissionId("pid"))
                .thenReturn(Optional.of(new SimplePermissionRequest("pid", "cid", "dnid", "cmId", "conid", state)));
        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/region-connectors/at-eda/permission-status/{permissionId}", "123")
                                .accept(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.permissionId", is("No permission with ID 123 found")));
    }

    @Test
    void createPermissionRequest_returnsPermissionRequest() throws Exception {
        // Given
        CreatedPermissionRequest expected = new CreatedPermissionRequest("pid", "cmRequestId");
        when(permissionRequestCreationService.createAndSendPermissionRequest(any()))
                .thenReturn(expected);
        LocalDate end = LocalDate.now(Clock.systemUTC()).minusDays(1);
        LocalDate start = end.minusDays(1);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation("cid", "0".repeat(33), "dnid", "0".repeat(8), start, end);

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/region-connectors/at-eda/permission-request")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("connectionId", permissionRequestForCreation.connectionId())
                                .param("start", permissionRequestForCreation.start().format(DateTimeFormatter.ISO_DATE))
                                .param("end", permissionRequestForCreation.end().format(DateTimeFormatter.ISO_DATE))
                                .param("dataNeedId", permissionRequestForCreation.dataNeedId())
                                .param("meteringPointId", permissionRequestForCreation.meteringPointId())
                                .param("dsoId", permissionRequestForCreation.dsoId())
                )
                // Then
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(expected)));
    }

    @Test
    void createPermissionRequest_400WhenStartAfterEndDate() throws Exception {
        // Given
        LocalDate end = LocalDate.now(Clock.systemUTC()).minusDays(1);
        LocalDate start = end.plusDays(1);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation("cid", "0".repeat(33), "dnid", "0".repeat(8), start, end);
        String content = objectMapper.writeValueAsString(permissionRequestForCreation);

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/region-connectors/at-eda/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content)
                )
                // Then
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPermissionRequest_400WhenStartDateNull() throws Exception {
        // Given
        LocalDate end = LocalDate.now(Clock.systemUTC()).minusDays(1);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation("cid", "0".repeat(33), "dnid", "0".repeat(8), null, end);
        String content = objectMapper.writeValueAsString(permissionRequestForCreation);

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/region-connectors/at-eda/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content)
                )
                // Then
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPermissionRequest_400WhenEndDateNull() throws Exception {
        // Given
        LocalDate start = LocalDate.now(Clock.systemUTC()).minusDays(1);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation("cid", "0".repeat(33), "dnid", "0".repeat(8), start, null);
        String content = objectMapper.writeValueAsString(permissionRequestForCreation);

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/region-connectors/at-eda/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content)
                )
                // Then
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("permissionRequestArguments")
    void createPermissionRequest_400WhenMissingStringParameters(String connectionId, String meteringPoint, String dataNeedsId, String dsoId) throws Exception {
        // Given
        LocalDate end = LocalDate.now(Clock.systemUTC()).minusDays(1);
        LocalDate start = end.minusDays(1);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(connectionId, meteringPoint, dataNeedsId, dsoId, start, end);
        String content = objectMapper.writeValueAsString(permissionRequestForCreation);

        // When
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/region-connectors/at-eda/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content)
                )
                // Then
                .andExpect(status().isBadRequest());
    }
}
