package energy.eddie.regionconnector.at.eda.permission.request.extensions;

import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.ponton.NoOpEdaAdapter;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest
class SavingExtensionTest {
    @Autowired
    private AtPermissionRequestRepository repository;
    // Stop Spring from trying to construct these beans
    @MockBean
    private ServletWebServerApplicationContext ignored;
    @MockBean
    private Supplier<Integer> alsoIgnored;

    @Test
    void extension_savesPermissionRequest() {
        // Given
        SavingExtension extension = new SavingExtension(repository);
        EdaPermissionRequest permissionRequest = new EdaPermissionRequest("cid", "dnid", mock(CCMORequest.class), new NoOpEdaAdapter());

        // When
        extension.accept(permissionRequest);

        // Then
        assertEquals(permissionRequest, repository.findByPermissionId(permissionRequest.permissionId()).get());
    }
}