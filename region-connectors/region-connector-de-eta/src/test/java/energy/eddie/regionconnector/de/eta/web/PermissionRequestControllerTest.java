package energy.eddie.regionconnector.de.eta.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.regionconnector.de.eta.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PermissionRequestController.class})
@Import(RegionConnectorsCommonControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class PermissionRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Outbox outbox;
    @MockBean
    private DataNeedCalculationService<DataNeedInterface> dataNeedCalculationService;

    @Test
    void createPermissionRequest_withUnknownDataNeed_returnsBadRequest() throws Exception {
        // Given
        when(dataNeedCalculationService.calculate(anyString())).thenReturn(new DataNeedNotFoundResult());
        var body = new PermissionRequestForCreation("unknown-data-need", "conn-1");

        // When/Then
        mockMvc.perform(post(PATH_PERMISSION_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString("No data need with ID 'unknown-data-need' found.")));
    }

    @Test
    void createPermissionRequest_withValidatedHistoricalData_result_returnsCreated() throws Exception {
        // Given
        var result = new ValidatedHistoricalDataDataNeedResult(
                List.of(Granularity.PT30M),
                new Timeframe(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
                new Timeframe(LocalDate.now().minusDays(1), LocalDate.now())
        );
        when(dataNeedCalculationService.calculate(anyString())).thenReturn(result);
        var body = new PermissionRequestForCreation("validated", "conn-2");

        // When/Then
        mockMvc.perform(post(PATH_PERMISSION_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated())
               .andExpect(header().string("Location", notNullValue()))
               .andExpect(jsonPath("$.permissionId", not(emptyOrNullString())));
    }
}
