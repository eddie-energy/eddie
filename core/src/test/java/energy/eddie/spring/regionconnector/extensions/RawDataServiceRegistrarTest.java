package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.core.CoreSpringConfig;
import energy.eddie.core.services.RawDataService;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RawDataServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(RawDataService.class);
        Optional<RawDataProvider> mockProvider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class, () -> new RawDataServiceRegistrar(null, mockService, "foo"));
        assertThrows(NullPointerException.class, () -> new RawDataServiceRegistrar(mockProvider, null, "foo"));
        assertThrows(NullPointerException.class, () -> new RawDataServiceRegistrar(mockProvider, mockService, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(RawDataService.class);

        // When
        new RawDataServiceRegistrar(Optional.empty(), mockService, "foo");

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(RawDataService.class);
        var mockProvider = mock(RawDataProvider.class);

        // When
        new RawDataServiceRegistrar(Optional.of(mockProvider), mockService, "foo");

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}

/**
 * Two test classes that test that the {@link RawDataServiceRegistrar} is only loaded if the according property is true.
 * Needs two test classes as the properties are evaluated when Spring starts.
 */
@SpringBootTest(classes = CoreSpringConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// We can reuse this basic properties file as it just enables the DK and ES region connector
@ActiveProfiles("common-controller-advice")
@TestPropertySource(properties = {"eddie.raw.data.output.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
class RawDataServiceRegistrarDisabledTest {
    @Autowired
    private WebApplicationContext applicationContext;
    @MockBean
    private DataNeedsService unusedDataNeedsService;
    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void givenPropertyFalse_registrarIsNotAddedToContext() {
        assertNotNull(applicationContext.getServletContext());
        Object attribute = applicationContext.getServletContext().getAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.dk-energinet");
        assertNotNull(attribute);
        assertThrows(NoSuchBeanDefinitionException.class, () ->
                ((WebApplicationContext) attribute).getBean(RawDataServiceRegistrar.class));
    }
}

@SpringBootTest(classes = CoreSpringConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("common-controller-advice")
@TestPropertySource(properties = {"eddie.raw.data.output.enabled=true", "spring.jpa.hibernate.ddl-auto=none"})
class RawDataServiceRegistrarEnabledTest {
    @Autowired
    private WebApplicationContext applicationContext;
    @MockBean
    private DataNeedsService unusedDataNeedsService;
    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void givenPropertyTrue_registrarIsAddedToContext() {
        assertNotNull(applicationContext.getServletContext());
        Object attribute = applicationContext.getServletContext().getAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.dk-energinet");
        assertNotNull(attribute);
        var bean = ((WebApplicationContext) attribute).getBean(RawDataServiceRegistrar.class);

        assertNotNull(bean);
    }
}
