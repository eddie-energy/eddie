package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.regionconnector.de.eta.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.de.eta.service.PermissionRequestCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PermissionRequestCreationService service;

    @InjectMocks
    private PermissionRequestController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createPermissionRequestShouldReturnCreated() throws Exception {
        CreatedPermissionRequest response = new CreatedPermissionRequest("perm-1");
        when(service.createPermissionRequest(any())).thenReturn(response);

        mockMvc.perform(post(PATH_PERMISSION_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"connectionId\": \"conn-1\", \"dataNeedId\": \"dn-1\", \"meteringPointId\": \"mp-1\"}"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permissionId").value("perm-1"));
    }
}
