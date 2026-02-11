// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.agnostic.aiida.AiidaPermissionRequestDto;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.web.DataNeedsAdvice;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.dtos.PermissionDetailsDto;
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
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static energy.eddie.regionconnector.aiida.web.PermissionRequestController.PATH_HANDSHAKE_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.aiida.web.PermissionRequestControllerTest.HMAC_SECRET;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = PermissionRequestController.class, properties = "eddie.jwt.hmac.secret=" + HMAC_SECRET)
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
@Import({PermissionRequestControllerTest.ControllerTestConfiguration.class, PermissionRequestControllerTest.TestJpaConfiguration.class})
class PermissionRequestControllerTest {
    static final String HMAC_SECRET = "RbNQrp0Dfd+fNoTalQQTd5MRurblhcDtVYaPGoDsg8Q=";
    private final UUID eddieId = UUID.fromString("a69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private final UUID permissionId = UUID.fromString("41d0a13e-688a-454d-acab-7a6b2951cde2");
    private final UUID aiidaId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AiidaPermissionService mockService;
    @SuppressWarnings("unused")
    @MockitoBean
    private AiidaPermissionEventRepository mockRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private AiidaPermissionRequestViewRepository mockViewRepository;
    @SuppressWarnings("unused")
    @MockitoBean
    private DataNeedsService unusedDataNeedsService;
    @SuppressWarnings("unused")
    @MockitoBean
    private MqttService unusedMqttService;

    @Test
    void givenNoRequestBody_createPermissionRequest_returnsBadRequests() throws Exception {
        mockMvc.perform(post("/permission-request").contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("Invalid request body.")));
    }

    @Test
    void givenMissingConnectionId_createPermissionRequest_returnsBadRequests() throws Exception {
        var json = "{\"dataNeedIds\":[\"1\"]}";

        mockMvc.perform(post("/permission-request").content(json).contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", is("connectionId: must not be blank")));
    }

    @Test
    void givenAdditionalNotNeededInformation_createPermissionRequests_isIgnored() throws Exception {
        // Given
        var qrCodeDto = new AiidaPermissionRequestDto(UUID.randomUUID(),
                                                      List.of(permissionId),
                                                      "http://localhost:8080/example",
                                                      "accessToken");
        when(mockService.createValidateAndSendPermissionRequests(any())).thenReturn(qrCodeDto);
        var requestJson = "{\"connectionId\":\"Hello My Test\",\"dataNeedIds\":[\"11\"],\"extra\":\"information\"}";

        // When
        mockMvc.perform(post("/permission-request").content(requestJson).contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.permissionIds[0]", is(permissionId.toString())));
        verify(mockService).createValidateAndSendPermissionRequests(any());
    }

    @Test
    void givenValidInput_createPermissionRequests_asExpected() throws Exception {
        // Given
        when(mockService.createValidateAndSendPermissionRequests(any())).thenReturn(new AiidaPermissionRequestDto(
                eddieId,
                List.of(permissionId),
                "http://localhost:8080/example",
                "accessToken"));
        var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedIds\":[\"1\"]}";

        // When
        mockMvc.perform(post("/permission-request").content(json).contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.permissionIds[0]", is(permissionId.toString())))
               .andExpect(jsonPath("$.accessToken", is("accessToken")))
               .andExpect(jsonPath("$.handshakeUrl", is("http://localhost:8080/example")));

        verify(mockService).createValidateAndSendPermissionRequests(any());
    }

    @Test
    void givenUnsupportedDataNeedId_createPermissionRequest_returnsBadRequests() throws Exception {
        // Given
        when(mockService.createValidateAndSendPermissionRequests(any())).thenThrow(new UnsupportedDataNeedException(
                AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                "test",
                "Is a test reason."));
        var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedIds\":[\"UNSUPPORTED\"]}";

        // When
        mockMvc.perform(post("/permission-request").content(json).contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath(ERRORS_JSON_PATH, iterableWithSize(1)))
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   is("Region connector 'aiida' does not support data need with ID 'test': Is a test reason.")));
    }

    @Test
    void givenInvalidOperation_updatePermissionRequest_returnsBadRequest() throws Exception {
        // Given
        var json = "{\"operation\":\"INVALID_VALUE_BLA\"}";

        // When
        mockMvc.perform(patch(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId).content(json)
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
        var json = "{\"operation\":\"REJECT\"}";

        // When
        mockMvc.perform(patch(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId).content(json)
                                                                              .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNoContent());
    }

    @Test
    void givenUnfulfillable_updatePermissionRequest_returnsNoContent() throws Exception {
        // Given
        var json = "{\"operation\":\"UNFULFILLABLE\"}";

        // When
        mockMvc.perform(patch(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId).content(json)
                                                                              .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isNoContent());
    }

    @Test
    void givenAccepted_updatePermissionRequest_callsServiceAndReturnsCredentials() throws Exception {
        // Given
        var json = "{\"operation\":\"ACCEPT\", \"aiidaId\":\"" + aiidaId + "\"}";
        when(mockService.acceptPermission(permissionId.toString(), aiidaId)).thenReturn(new MqttDto(
                "tcp://localhost:1883",
                permissionId.toString(),
                "MySuperSafePassword",
                "data",
                "status",
                "termination"));

        // When
        mockMvc.perform(patch(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId).content(json)
                                                                              .contentType(MediaType.APPLICATION_JSON))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.username", is(permissionId.toString())))
               .andExpect(jsonPath("$.password", is("MySuperSafePassword")))
               .andExpect(jsonPath("$.dataTopic", is("data")))
               .andExpect(jsonPath("$.statusTopic", is("status")))
               .andExpect(jsonPath("$.terminationTopic", is("termination")));
    }

    @Test
    void getPermissionDetails_returnsAsExpected() throws Exception {
        // Given
        when(mockService.detailsForPermission(permissionId.toString())).thenReturn(new PermissionDetailsDto(eddieId,
                                                                                                            createDummyRequest(),
                                                                                                            new DummyAiidaDataNeed()));

        // When
        mockMvc.perform(get(PATH_HANDSHAKE_PERMISSION_REQUEST, permissionId))
               // Then
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.permission_request").exists())
               .andExpect(jsonPath("$.permission_request.permission_id", is(permissionId.toString())))
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

        setField(request, "permissionId", permissionId.toString());
        setField(request, "connectionId", "someConnectionId");
        setField(request, "dataNeedId", "someDataNeedId");
        setField(request, "start", LocalDate.of(2000, 1, 1));
        setField(request, "end", LocalDate.of(9999, 12, 31));
        setField(request, "status", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        setField(request, "terminationTopic", "someTopic");
        setField(request, "message", "someMessage");
        setField(request, "created", Instant.now());

        return request;
    }

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

    /**
     * Needed a fake JPA configuration to let spring create an entity manager for the tests to run.
     */
    @TestConfiguration
    public static class TestJpaConfiguration {
        @Bean
        public DataSource dataSource() {
            return DataSourceBuilder.create()
                                    .driverClassName("org.h2.Driver")
                                    .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                                    .username("sa")
                                    .password("")
                                    .build();
        }

        @Bean
        public JpaVendorAdapter jpaVendorAdapter() {
            HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
            adapter.setDatabase(Database.H2);
            adapter.setGenerateDdl(true);
            adapter.setShowSql(false);
            return adapter;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(
                DataSource dataSource,
                JpaVendorAdapter jpaVendorAdapter
        ) {
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);
            // Set packages to scan if needed:
            emf.setPackagesToScan("energy.eddie"); // adjust this package to where your entities are
            emf.setJpaVendorAdapter(jpaVendorAdapter);
            return emf;
        }
    }

    private static class DummyAiidaDataNeed extends AiidaDataNeed {}
}
