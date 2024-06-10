package energy.eddie.examples.newexampleapp.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private Model model;
    private HomeController homeController;

    @BeforeEach
    public void setup() {
        homeController = new HomeController();
    }

    @Test
    void testHome() {
        String viewName = homeController.home(model);
        verify(model, times(1)).addAttribute(eq("title"), anyString());

        assertEquals("index", viewName);
    }
}
