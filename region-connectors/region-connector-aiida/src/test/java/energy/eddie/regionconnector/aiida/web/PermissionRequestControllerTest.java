package energy.eddie.regionconnector.aiida.web;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.web.DataNeedsAdvice;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.mqtt.MqttDto;
import energy.eddie.regionconnector.aiida.mqtt.MqttService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.aiida.web.PermissionRequestController.PATH_UPDATE_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.aiida.web.PermissionRequestControllerTest.HMAC_SECRET;
import static energy.eddie.regionconnector.shared.security.JwtUtil.JWS_ALGORITHM;
import static energy.eddie.regionconnector.shared.security.JwtUtil.JWT_PERMISSIONS_CLAIM;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = PermissionRequestController.class, properties = "eddie.jwt.hmac.secret=" + HMAC_SECRET)
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class PermissionRequestControllerTest {
    static final String HMAC_SECRET = "RbNQrp0Dfd+fNoTalQQTd5MRurblhcDtVYaPGoDsg8Q=";
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AiidaPermissionService service;
    @MockBean
    private AiidaPermissionEventRepository mockRepository;
    @MockBean
    private AiidaPermissionRequestViewRepository mockViewRepository;
    @MockBean
    private DataNeedsService unusedDataNeedsService;
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
        public JwtUtil jwtUtil(@Value("${eddie.jwt.hmac.secret}") String jwtHmacSecret) {
            return new JwtUtil(jwtHmacSecret);
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
        var mockDto = mock(PermissionDto.class);
        when(service.createValidateAndSendPermissionRequest(any())).thenReturn(mockDto);
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
        verify(service).createValidateAndSendPermissionRequest(any());
    }

    @Test
    void givenValidInput_createPermissionRequest_asExpected() throws Exception {
        // Given
        var permissionId = "SecondSomeId";
        var mockDto = mock(PermissionDto.class);
        when(service.createValidateAndSendPermissionRequest(any())).thenReturn(mockDto);
        when(mockDto.permissionId()).thenReturn(permissionId);
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
               .andExpect(jsonPath("$.permissionId", is(permissionId)));

        verify(service).createValidateAndSendPermissionRequest(any());
    }

    @Test
    void givenUnsupportedDataNeedId_createPermissionRequest_returnsBadRequest() throws Exception {
        // Given
        when(service.createValidateAndSendPermissionRequest(any())).thenThrow(new UnsupportedDataNeedException(
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
    void givenValidInput_createPermissionRequest_returnsJwtWithOnlyNewPermissionId() throws Exception {
        // Given
        var newPermissionId = "TestOnlyNewPermissionId";
        var mockDto = mock(PermissionDto.class);
        when(service.createValidateAndSendPermissionRequest(any())).thenReturn(mockDto);
        when(mockDto.permissionId()).thenReturn(newPermissionId);
        var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"1\"}";

        // When
        String response = mockMvc.perform(post("/permission-request")
                                                  .content(json)
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .header(HttpHeaders.AUTHORIZATION,
                                                          // this JWT contains other permission IDs as well
                                                          "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTQwMzUwMjYsInBlcm1pc3Npb25zIjp7ImVzLWRhdGFkaXMiOlsiZm9vIiwiYmFyIl0sImRrLWVuZXJnaW5ldCI6WyJNdXN0Iiwic3RpbGwiLCJiZSIsInByZXNlbnQiXSwiYWlpZGEiOlsiYW5vdGhlcklkIl19fQ.CtYysLrv90-nkH3D2KhzXOmbqkIF--Up_QZGxbf9npQ"))
                                 // Then
                                 .andExpect(status().isCreated())
                                 .andReturn().getResponse().getContentAsString();

        String jwtValue = JsonPath.read(response, "$.accessToken");

        Map<String, List<String>> permissions = extractPermissionsFromJwt(jwtValue);

        assertEquals(1, permissions.size());
        assertEquals(newPermissionId, permissions.get("aiida").getFirst());
    }

    private Map<String, List<String>> extractPermissionsFromJwt(String jwt) throws BadJOSEException, ParseException, JOSEException {
        ImmutableSecret<SecurityContext> immutableSecret = new ImmutableSecret<>(Base64.getDecoder()
                                                                                       .decode(HMAC_SECRET));
        var keySelector = new JWSVerificationKeySelector<>(JWS_ALGORITHM, immutableSecret);
        var defaultProcessor = new DefaultJWTProcessor<>();
        defaultProcessor.setJWSKeySelector(keySelector);

        JWTClaimsSet claims = defaultProcessor.process(jwt, null);
        return (Map<String, List<String>>) claims.getClaim(JWT_PERMISSIONS_CLAIM);
    }

    @Test
    void givenInvalidOperation_updatePermissionRequest_returnsBadRequest() throws Exception {
        // Given
        var permissionId = "someTestId";
        var json = "{\"operation\":\"INVALID_VALUE_BLA\"}";

        // When
        mockMvc.perform(patch(PATH_UPDATE_PERMISSION_REQUEST, permissionId)
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
        mockMvc.perform(patch(PATH_UPDATE_PERMISSION_REQUEST, permissionId)
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
        mockMvc.perform(patch(PATH_UPDATE_PERMISSION_REQUEST, permissionId)
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
        when(service.acceptPermission(permissionId)).thenReturn(new MqttDto(permissionId,
                                                                            "MySuperSafePassword",
                                                                            "data",
                                                                            "status",
                                                                            "termination"));

        // When
        mockMvc.perform(patch(PATH_UPDATE_PERMISSION_REQUEST, permissionId)
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
}
