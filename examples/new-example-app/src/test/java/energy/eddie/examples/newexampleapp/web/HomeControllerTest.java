package energy.eddie.examples.newexampleapp.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {
    @Mock
    private Model model;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private HomeController homeController;

    @Captor
    private ArgumentCaptor<Cookie> cookieCaptor;

    @BeforeEach
    public void setup() {
        homeController = new HomeController();
    }

    @Test
    void testHome() {
        // Mocking HttpServletRequest to simulate no cookies present
        when(request.getCookies()).thenReturn(null);

        // Call the method under test
        String viewName = homeController.home(model, request, response);

        // Verify the returned view name
        assertEquals("index", viewName);

        // Verify that attributes are added to the model
        verify(model, times(1)).addAttribute(eq("connectionIDHistorical"), anyString());
        verify(model, times(1)).addAttribute(eq("connectionIDAiida"), anyString());
        verify(model, times(1)).addAttribute("dataNeedID","b35dc783-a25e-487a-a914-6064bdd7feb5");
        verify(model, times(1)).addAttribute("aiidaID","5dc71d7e-e8cd-4403-a3a8-d3c095c97a84");
    }

    @Test
    void testCreateCookie() {
        String cookieName = "testCookie";
        String connectionID = homeController.createCookie(response, null, cookieName);

        // Verify that a cookie was added to the response
        verify(response).addCookie(cookieCaptor.capture());
        Cookie capturedCookie = cookieCaptor.getValue();

        // Assert that connectionID starts with "CONN_"
        assertTrue(connectionID.startsWith("CONN_"));

        // Assert that the connectionID has the expected length
        // "CONN_" (5 chars) + UUID (36 chars) = 41 chars
        assertEquals(41, connectionID.length());

        // Assert cookie properties
        assertEquals(cookieName, capturedCookie.getName());
        assertEquals(connectionID, capturedCookie.getValue());
    }
}
