package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.web.DataNeedsAdvice;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.dtos.PermissionDetailsDto;
import energy.eddie.regionconnector.aiida.dtos.QrCodeDto;
import energy.eddie.regionconnector.aiida.mqtt.MqttDto;
import energy.eddie.regionconnector.aiida.mqtt.MqttService;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionEventRepository;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.aiida.services.AiidaPermissionService;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.aiida.web.PermissionRequestController.PATH_HANDSHAKE_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.aiida.web.PermissionRequestControllerTest.HMAC_SECRET;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = PermissionRequestController.class, properties = "eddie.jwt.hmac.secret=" + HMAC_SECRET)
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionRequestControllerTest {
    static final String HMAC_SECRET = "RbNQrp0Dfd+fNoTalQQTd5MRurblhcDtVYaPGoDsg8Q=";
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AiidaPermissionService mockService;
    @SuppressWarnings("unused")
    @MockBean
    private AiidaPermissionEventRepository mockRepository;
    @SuppressWarnings("unused")
    @MockBean
    private AiidaPermissionRequestViewRepository mockViewRepository;
    @SuppressWarnings("unused")
    @MockBean
    private DataNeedsService unusedDataNeedsService;
    @SuppressWarnings("unused")
    @MockBean
    private MqttService unusedMqttService;

    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
            return new RegionConnectorsCommonControllerAdvice();
        }

        @Bean
        public DataNeedsAdvice dataNeedsAdvice() {
            return new DataNeedsAdvice();
        }

        @Bean
        public JwtUtil jwtUtil(
                @Value("${eddie.jwt.hmac.secret}") String jwtHmacSecret,
                @Value("${eddie.permission.request.timeout.duration}") int timeoutDuration
        ) {
            return new JwtUtil(jwtHmacSecret, timeoutDuration);
        }
    }

    @Test
    void givenNoRequestBody_createPermissionRequest_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    void givenMissingConnectionId_createPermissionRequest_returnsBadRequest() throws Exception {
        var json = "{\"dataNeedId\":\"1\"}";

        mockMvc.perform(post("/permission-request")
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("connectionId: must not be blank")));
    }

    @Test
    void givenAdditionalNotNeededInformation_createPermissionRequest_isIgnored() throws Exception {
        // Given
        var permissionId = "SomeId";
        var mockDto = mock(QrCodeDto.class);
        when(mockService.createValidateAndSendPermissionRequest(any())).thenReturn(mockDto);
        when(mockDto.permissionId()).thenReturn(permissionId);
        var requestJson = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"11\",\"extra\":\"information\"}";
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId)
                                                                                            .toString();

        // When
        mockMvc.perform(post("/permission-request")
                                .content(requestJson)
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isCreated())
               .andExpect(header().string("Location", is(expectedLocationHeader)))
               .andExpect(jsonPath("$.permissionId", is(permissionId)));
        verify(mockService).createValidateAndSendPermissionRequest(any());
    }

    @Test
    void givenValidInput_createPermissionRequest_asExpected() throws Exception {
        // Given
        var permissionId = "SecondSomeId";
        when(mockService.createValidateAndSendPermissionRequest(any())).thenReturn(new QrCodeDto(permissionId,
                                                                                                 "serviceName",
                                                                                                 "http://localhost:8080/example",
                                                                                                 "token"));
        var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"1\"}";
        var expectedLocationHeader = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionId)
                                                                                            .toString();

        // When
        mockMvc.perform(post("/permission-request")
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isCreated())
               .andExpect(header().string("Location", is(expectedLocationHeader)))
               .andExpect(jsonPath("$.permissionId", is(permissionId)))
               .andExpect(jsonPath("$.serviceName", is("serviceName")))
               .andExpect(jsonPath("$.handshakeUrl", is("http://localhost:8080/example")))
               .andExpect(jsonPath("$.accessToken", is("token")));

        verify(mockService).createValidateAndSendPermissionRequest(any());
    }

    @Test
    void givenUnsupportedDataNeedId_createPermissionRequest_returnsBadRequest() throws Exception {
        // Given
        when(mockService.createValidateAndSendPermissionRequest(any())).thenThrow(new UnsupportedDataNeedException(
                AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                "test",
                "Is a test reason."));
        var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"UNSUPPORTED\"}";

        // When
        mockMvc.perform(post("/permission-request")
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   is("Region connector 'aiida' does not support data need with ID 'test': Is a test reason.")));
    }

    @Test
    void givenInvalidOperation_updatePermissionRequest_returnsBadRequest() throws Exception {
        // Given
        var permissionId = "someTestId";
        var json = "{\"operation\":\"INVALID_VALUE_BLA\"}";

        // When
        mockMvc.perform(patch(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId)
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   is("operation: Invalid enum value: 'INVALID_VALUE_BLA'. Valid values: [ACCEPT, REJECT, UNFULFILLABLE].")));
    }

    @Test
    void givenRejected_updatePermissionRequest_returnsNoContent() throws Exception {
        // Given
        var permissionId = "someTestId";
        var json = "{\"operation\":\"REJECT\"}";

        // When
        mockMvc.perform(patch(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId)
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNoContent());
    }

    @Test
    void givenUnfulfillable_updatePermissionRequest_returnsNoContent() throws Exception {
        // Given
        var permissionId = "someTestId";
        var json = "{\"operation\":\"UNFULFILLABLE\"}";

        // When
        mockMvc.perform(patch(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId)
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNoContent());
    }

    @Test
    void givenAccepted_updatePermissionRequest_callsServiceAndReturnsCredentials() throws Exception {
        // Given
        var permissionId = "someTestId";
        var json = "{\"operation\":\"ACCEPT\"}";
        when(mockService.acceptPermission(permissionId)).thenReturn(new MqttDto(permissionId,
                                                                                "MySuperSafePassword",
                                                                                "data",
                                                                                "status",
                                                                                "termination"));

        // When
        mockMvc.perform(patch(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId)
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.username", is(permissionId)))
               .andExpect(jsonPath("$.password", is("MySuperSafePassword")))
               .andExpect(jsonPath("$.dataTopic", is("data")))
               .andExpect(jsonPath("$.statusTopic", is("status")))
               .andExpect(jsonPath("$.terminationTopic", is("termination")));
    }

    @Test
    void getPermissionDetails_returnsAsExpected() throws Exception {
        // Given
        var permissionId = "someTestId";
        when(mockService.detailsForPermission(permissionId)).thenReturn(
                new PermissionDetailsDto(createDummyRequest(), new DummyAiidaDataNeed()));

        // When
        mockMvc.perform(get(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.permission_request").exists())
               .andExpect(jsonPath("$.permission_request.permission_id", is("somePermissionId")))
               .andExpect(jsonPath("$.permission_request.data_need_id", is("someDataNeedId")))
               .andExpect(jsonPath("$.permission_request.start", is("2000-01-01")))
               .andExpect(jsonPath("$.permission_request.end", is("9999-12-31")))
               .andExpect(jsonPath("$.permission_request.connection_id", is("someConnectionId")))
               .andExpect(jsonPath("$.permission_request.status").doesNotExist())
               .andExpect(jsonPath("$.permission_request.terminationTopic").doesNotExist())
               .andExpect(jsonPath("$.permission_request.termination_topic").doesNotExist());
    }

    private AiidaPermissionRequestInterface createDummyRequest() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?> constructor = AiidaPermissionRequest.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);

        AiidaPermissionRequest request = (AiidaPermissionRequest) constructor.newInstance();

        setField(request, "permissionId", "somePermissionId");
        setField(request, "connectionId", "someConnectionId");
        setField(request, "dataNeedId", "someDataNeedId");
        setField(request, "start", LocalDate.of(2000, 1, 1));
        setField(request, "end", LocalDate.of(9999, 12, 31));
        setField(request, "status", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        setField(request, "terminationTopic", "someTopic");
        setField(request, "mqttUsername", "someUsername");
        setField(request, "message", "someMessage");
        setField(request, "created", Instant.now());

        return request;
    }

    private static class DummyAiidaDataNeed extends AiidaDataNeed {}
}
