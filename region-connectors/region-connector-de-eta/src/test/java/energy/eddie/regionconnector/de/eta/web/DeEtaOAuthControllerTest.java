package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.oauth.DeEtaOAuthProperties;
import energy.eddie.regionconnector.de.eta.oauth.DeEtaOAuthStateStore;
import energy.eddie.regionconnector.de.eta.permission.requests.DateRange;
import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeEtaOAuthController.class)
@Import({DeEtaOAuthControllerTest.TestConfig.class, RegionConnectorsCommonControllerAdvice.class})
@AutoConfigureMockMvc(addFilters = false)
class DeEtaOAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeEtaOAuthStateStore stateStore;

    @MockitoBean
    private DeEtaPermissionRequestRepository repository;

    @Test
    void authorize_whenPermissionValid_returns302AndPersistsState() throws Exception {
        UUID permissionId = UUID.randomUUID();
        var perm = new DeEtaPermissionRequest(
                permissionId.toString(),
                "conn-123",
                "data-need-1",
                PermissionProcessStatus.VALIDATED,
                ZonedDateTime.now(),
                new DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
                "PT30M"
        );
        when(repository.findByPermissionId(anyString())).thenReturn(Optional.of(perm));

        var mvcResult = mockMvc.perform(get("/region-connectors/de-eta/authorize/" + permissionId))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", allOf(
                        containsString("client_id=test-client"),
                        containsString("redirect_uri="),
                        containsString("response_type=code"),
                        containsString("scope=permissions.read%20permissions.write"),
                        containsString("state=")
                )))
                .andReturn();

        String location = mvcResult.getResponse().getHeader("Location");
        assertThat(location, notNullValue());
        var uri = URI.create(location);
        var query = uri.getQuery();
        assertThat(query, containsString("state="));
        String state = query.split("state=")[1].split("&")[0];

        // Ensure state persisted
        assertThat(stateStore.find(state).isPresent(), is(true));
        assertThat(stateStore.find(state).get().permissionId().toString(), equalTo(permissionId.toString()));
    }

    @Test
    void authorize_whenPermissionNotFound_returns404() throws Exception {
        when(repository.findByPermissionId(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(get("/region-connectors/de-eta/authorize/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void authorize_whenPermissionWrongState_returns409() throws Exception {
        UUID permissionId = UUID.randomUUID();
        var perm = new DeEtaPermissionRequest(
                permissionId.toString(),
                "conn-123",
                "data-need-1",
                PermissionProcessStatus.CREATED,
                ZonedDateTime.now(),
                new DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
                "PT30M"
        );
        when(repository.findByPermissionId(anyString())).thenReturn(Optional.of(perm));

        mockMvc.perform(get("/region-connectors/de-eta/authorize/" + permissionId))
                .andExpect(status().isConflict());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        DeEtaOAuthProperties deEtaOAuthProperties() {
            return new DeEtaOAuthProperties(
                    "test-client",
                    "secret-do-not-log",
                    "https://eta-plus.com/oauth/authorize",
                    "https://eta-plus.com/oauth/token",
                    "https://eddie.example.com/region-connectors/de-eta/callback",
                    "permissions.read permissions.write",
                    true
            );
        }

        @Bean
        DeEtaOAuthStateStore stateStore() {
            return new DeEtaOAuthStateStore();
        }
    }
}
